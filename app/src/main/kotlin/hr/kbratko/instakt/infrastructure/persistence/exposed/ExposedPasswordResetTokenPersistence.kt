package hr.kbratko.instakt.infrastructure.persistence.exposed

import arrow.core.Either
import arrow.core.Option
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.singleOrNone
import hr.kbratko.instakt.domain.DbError.PasswordResetTokenStillValid
import hr.kbratko.instakt.domain.DbError.UnknownPasswordResetToken
import hr.kbratko.instakt.domain.DomainError
import hr.kbratko.instakt.domain.config.DefaultInstantProvider
import hr.kbratko.instakt.domain.utility.getOrRaise
import hr.kbratko.instakt.domain.model.User
import hr.kbratko.instakt.domain.persistence.PasswordResetTokenPersistence
import hr.kbratko.instakt.domain.security.Token
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
import kotlin.time.Duration
import org.jetbrains.exposed.sql.ReferenceOption.CASCADE as Cascade
import org.jetbrains.exposed.sql.SchemaUtils.create as createIfNotExists

object PasswordResetTokensTable : UUIDTable("password_reset_tokens", "password_reset_token_pk") {
    val userId = reference("user_fk", UsersTable, onDelete = Cascade)
    val createdAt = timestampWithTimeZone("created_at")
    val expiresAt = timestampWithTimeZone("expires_at")
}

data class PasswordResetTokenPersistenceConfig(val expiresAfter: Duration)

fun ExposedPasswordResetTokenPersistence(db: Database, config: PasswordResetTokenPersistenceConfig) =
    object : PasswordResetTokenPersistence {
        init {
            transaction {
                createIfNotExists(PasswordResetTokensTable)
            }
        }

        override suspend fun insert(userId: User.Id): Either<DomainError, Token.PasswordReset> = either {
            ioTransaction(db = db) {
                PasswordResetTokensTable
                    .select(PasswordResetTokensTable.expiresAt)
                    .where { PasswordResetTokensTable.userId eq userId.value }
                    .singleOrNone()
                    .fold(
                        ifEmpty = { cleanAndInsert(userId) },
                        ifSome = { raise(PasswordResetTokenStillValid) }
                    )
            }
        }

        override suspend fun selectUserId(token: Token.PasswordReset): Option<User.Id> = ioTransaction(db = db) {
            token.value.toUUIDOrNone()
                .flatMap { passwordResetToken ->
                    PasswordResetTokensTable
                        .select(PasswordResetTokensTable.userId)
                        .where { PasswordResetTokensTable.id eq passwordResetToken }
                        .singleOrNone()
                        .map { User.Id(it[PasswordResetTokensTable.userId].value) }
                }
        }

        override suspend fun delete(token: Token.PasswordReset): Either<DomainError, Token.PasswordReset> = either {
            ioTransaction(db = db) {
                val passwordResetTokenId = token.value.toUUIDOrNone().getOrRaise { UnknownPasswordResetToken }
                val deletedCount = PasswordResetTokensTable.deleteWhere {
                    PasswordResetTokensTable.id eq passwordResetTokenId
                }
                ensure(deletedCount > 0) { UnknownPasswordResetToken }

                token
            }
        }

        private fun cleanAndInsert(userId: User.Id, now: Instant = DefaultInstantProvider.now()): Token.PasswordReset {
            PasswordResetTokensTable.deleteWhere { PasswordResetTokensTable.userId eq userId.value }

            val id = PasswordResetTokensTable.insertAndGetId {
                it[this.userId] = userId.value
                it[this.createdAt] = now.toJavaInstant().atOffset(UTC)
                it[expiresAt] = (now + config.expiresAfter).toJavaInstant().atOffset(UTC)
            }

            return Token.PasswordReset(id.value.toString())
        }
    }
