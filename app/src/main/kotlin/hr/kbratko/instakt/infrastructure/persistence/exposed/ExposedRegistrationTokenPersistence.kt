package hr.kbratko.instakt.infrastructure.persistence.exposed

import arrow.core.Either
import arrow.core.Option
import arrow.core.raise.either
import arrow.core.singleOrNone
import hr.kbratko.instakt.domain.DbError.InvalidRegistrationToken
import hr.kbratko.instakt.domain.DbError.RegistrationTokenAlreadyConfirmed
import hr.kbratko.instakt.domain.DbError.RegistrationTokenExpired
import hr.kbratko.instakt.domain.DbError.RegistrationTokenStillValid
import hr.kbratko.instakt.domain.DomainError
import hr.kbratko.instakt.domain.config.DefaultInstantProvider
import hr.kbratko.instakt.domain.conversion.ConversionScope
import hr.kbratko.instakt.domain.conversion.convert
import hr.kbratko.instakt.domain.getOrRaise
import hr.kbratko.instakt.domain.model.RegistrationToken
import hr.kbratko.instakt.domain.model.RegistrationToken.Status.Confirmed
import hr.kbratko.instakt.domain.model.RegistrationToken.Status.Unconfirmed
import hr.kbratko.instakt.domain.persistence.RegistrationTokenPersistence
import hr.kbratko.instakt.domain.security.Token
import hr.kbratko.instakt.domain.security.toUUIDOrNone
import hr.kbratko.instakt.domain.toKotlinInstant
import java.time.ZoneOffset.UTC
import java.util.UUID
import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.kotlin.datetime.timestampWithTimeZone
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import kotlin.time.Duration
import org.jetbrains.exposed.sql.SchemaUtils.create as createIfNotExists

object RegistrationTokensTable : UUIDTable("registration_tokens", "registration_token_pk") {
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

        override suspend fun insert(): Token.Register = ioTransaction(db = db) {
            val id = RegistrationTokensTable.insertAndGetId {
                val createdAt = DefaultInstantProvider.now()

                it[this.createdAt] = createdAt.toJavaInstant().atOffset(UTC)
                it[expiresAt] = (createdAt + config.expiresAfter).toJavaInstant().atOffset(UTC)
            }

            Token.Register(id.value.toString())
        }

        override suspend fun select(token: Token.Register): Option<RegistrationToken> = ioTransaction(db = db) {
            token.toUUIDOrNone()
                .flatMap { id ->
                    RegistrationTokensTable
                        .selectAll()
                        .where { RegistrationTokensTable.id eq id }
                        .singleOrNone()
                        .map { it.convert(ResultRowToRegistrationTokenConversion) }
                }
        }

        override suspend fun confirm(token: Token.Register): Either<DomainError, Token.Register> = either {
            ioTransaction(db = db) {
                val (id, status, expiresAt) = selectTokenStatusAndExpiresAt(token).getOrRaise { InvalidRegistrationToken }

                when (status) {
                    is Unconfirmed -> {
                        val now = DefaultInstantProvider.now()

                        if (now > expiresAt)
                            raise(RegistrationTokenExpired)

                        RegistrationTokensTable.update({ RegistrationTokensTable.id eq id }) {
                            it[confirmedAt] = now.toJavaInstant().atOffset(UTC)
                        }

                        token
                    }

                    is Confirmed -> {
                        raise(RegistrationTokenAlreadyConfirmed)
                    }
                }
            }
        }

        override suspend fun delete(token: Token.Register): Either<DomainError, Token.Register> = either {
            ioTransaction(db = db) {
                val (id, status, expiresAt) = selectTokenStatusAndExpiresAt(token).getOrRaise { InvalidRegistrationToken }

                when (status) {
                    is Unconfirmed -> {
                        val now = DefaultInstantProvider.now()

                        if (now <= expiresAt)
                            raise(RegistrationTokenStillValid)

                        RegistrationTokensTable.deleteWhere { RegistrationTokensTable.id eq id }

                        token
                    }

                    is Confirmed -> {
                        raise(RegistrationTokenAlreadyConfirmed)
                    }
                }
            }
        }

        private fun selectTokenStatusAndExpiresAt(token: Token.Register): Option<Triple<UUID, RegistrationToken.Status, Instant>> =
            token.toUUIDOrNone()
                .flatMap { id ->
                    RegistrationTokensTable
                        .select(
                            RegistrationTokensTable.confirmedAt,
                            RegistrationTokensTable.expiresAt
                        )
                        .where { RegistrationTokensTable.id eq id }
                        .singleOrNone()
                        .map {
                            Triple(
                                id,
                                it.convert(ResultRowToRegistrationTokenStatusConversion),
                                it[RegistrationTokensTable.expiresAt].toKotlinInstant()
                            )
                        }
                }
    }

typealias ResultRowToRegistrationTokenConversionScope = ConversionScope<ResultRow, RegistrationToken>

val ResultRowToRegistrationTokenConversion = ResultRowToRegistrationTokenConversionScope {
    RegistrationToken(
        Token.Register(this[RegistrationTokensTable.id].value.toString()),
        this[RegistrationTokensTable.createdAt].toKotlinInstant(),
        this[RegistrationTokensTable.expiresAt].toKotlinInstant(),
        this.convert(ResultRowToRegistrationTokenStatusConversion)
    )
}

typealias ResultRowToRegistrationTokenStatusConversionScope = ConversionScope<ResultRow, RegistrationToken.Status>

val ResultRowToRegistrationTokenStatusConversion = ResultRowToRegistrationTokenStatusConversionScope {
    this[RegistrationTokensTable.confirmedAt]
        ?.let { Confirmed(it.toKotlinInstant()) }
        ?: Unconfirmed
}
