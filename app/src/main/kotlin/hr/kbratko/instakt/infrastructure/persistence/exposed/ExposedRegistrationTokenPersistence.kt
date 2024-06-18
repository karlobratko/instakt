package hr.kbratko.instakt.infrastructure.persistence.exposed

import arrow.core.Either
import arrow.core.None
import arrow.core.Some
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.singleOrNone
import hr.kbratko.instakt.domain.DbError.RegistrationTokenAlreadyConfirmed
import hr.kbratko.instakt.domain.DbError.RegistrationTokenExpired
import hr.kbratko.instakt.domain.DbError.RegistrationTokenStillValid
import hr.kbratko.instakt.domain.DbError.UnknownRegistrationToken
import hr.kbratko.instakt.domain.DomainError
import hr.kbratko.instakt.domain.config.DefaultInstantProvider
import hr.kbratko.instakt.domain.utility.getOrRaise
import hr.kbratko.instakt.domain.model.User
import hr.kbratko.instakt.domain.persistence.RegistrationTokenPersistence
import hr.kbratko.instakt.domain.security.Token
import hr.kbratko.instakt.domain.utility.toKotlinInstant
import hr.kbratko.instakt.domain.utility.toKotlinInstantOrNone
import hr.kbratko.instakt.domain.utility.toUUIDOrNone
import java.time.ZoneOffset.UTC
import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.kotlin.datetime.timestampWithTimeZone
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import kotlin.time.Duration
import org.jetbrains.exposed.sql.ReferenceOption.CASCADE as Cascade
import org.jetbrains.exposed.sql.SchemaUtils.create as createIfNotExists

object RegistrationTokensTable : UUIDTable("registration_tokens", "registration_token_pk") {
    val userId = reference("user_fk", UsersTable, onDelete = Cascade)
    val createdAt = timestampWithTimeZone("created_at")
    val expiresAt = timestampWithTimeZone("expires_at")
    val confirmedAt = timestampWithTimeZone("confirmed_at").nullable()
}

data class RegistrationTokenPersistenceConfig(val expiresAfter: Duration)

fun ExposedRegistrationTokenPersistence(db: Database, config: RegistrationTokenPersistenceConfig) =
    object : RegistrationTokenPersistence {
        init {
            transaction {
                createIfNotExists(RegistrationTokensTable)
            }
        }

        override suspend fun insert(userId: User.Id): Either<DomainError, Token.Register> = either {
            ioTransaction(db = db) {
                RegistrationTokensTable
                    .select(
                        RegistrationTokensTable.confirmedAt,
                        RegistrationTokensTable.expiresAt
                    )
                    .where { RegistrationTokensTable.userId eq userId.value }
                    .singleOrNone()
                    .fold(
                        ifEmpty = { cleanAndInsert(userId) },
                        ifSome = { row ->
                            row[RegistrationTokensTable.confirmedAt].toKotlinInstantOrNone()
                                .fold(
                                    ifEmpty = {
                                        val expiresAt = row[RegistrationTokensTable.expiresAt].toKotlinInstant()
                                        val now = DefaultInstantProvider.now()
                                        ensure(now > expiresAt) { RegistrationTokenStillValid }

                                        cleanAndInsert(userId, now)
                                    },
                                    ifSome = { raise(RegistrationTokenAlreadyConfirmed) }
                                )
                        }
                    )
            }
        }

        override suspend fun confirm(token: Token.Register): Either<DomainError, Token.Register> = either {
            ioTransaction(db = db) {
                val registrationToken = token.value.toUUIDOrNone().getOrRaise { UnknownRegistrationToken }
                val (status, expiresAt) = RegistrationTokensTable
                    .select(
                        RegistrationTokensTable.confirmedAt,
                        RegistrationTokensTable.expiresAt
                    )
                    .where { RegistrationTokensTable.id eq registrationToken }
                    .singleOrNone()
                    .map { row ->
                        Pair(
                            row[RegistrationTokensTable.confirmedAt].toKotlinInstantOrNone(),
                            row[RegistrationTokensTable.expiresAt].toKotlinInstant()
                        )
                    }
                    .getOrRaise { UnknownRegistrationToken }

                when (status) {
                    None -> {
                        val now = DefaultInstantProvider.now()
                        ensure(now <= expiresAt) { RegistrationTokenExpired }

                        RegistrationTokensTable.update({ RegistrationTokensTable.id eq registrationToken }) {
                            it[confirmedAt] = now.toJavaInstant().atOffset(UTC)
                        }

                        token
                    }

                    is Some -> {
                        raise(RegistrationTokenAlreadyConfirmed)
                    }
                }
            }
        }

        private fun cleanAndInsert(userId: User.Id, now: Instant = DefaultInstantProvider.now()): Token.Register {
            RegistrationTokensTable.deleteWhere { RegistrationTokensTable.userId eq userId.value }

            val id = RegistrationTokensTable.insertAndGetId {
                it[this.userId] = userId.value
                it[this.createdAt] = now.toJavaInstant().atOffset(UTC)
                it[expiresAt] = (now + config.expiresAfter).toJavaInstant().atOffset(UTC)
            }

            return Token.Register(id.value.toString())
        }
    }
