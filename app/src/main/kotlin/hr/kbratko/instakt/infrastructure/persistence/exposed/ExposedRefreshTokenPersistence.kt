package hr.kbratko.instakt.infrastructure.persistence.exposed

import arrow.core.Either
import arrow.core.Option
import arrow.core.Option.Companion.catch
import arrow.core.raise.either
import arrow.core.singleOrNone
import hr.kbratko.instakt.domain.DbError.InvalidRefreshToken
import hr.kbratko.instakt.domain.DbError.RefreshTokenAlreadyRevoked
import hr.kbratko.instakt.domain.DbError.RefreshTokenStillValid
import hr.kbratko.instakt.domain.DomainError
import hr.kbratko.instakt.domain.config.RoundedInstantProvider
import hr.kbratko.instakt.domain.conversion.ConversionScope
import hr.kbratko.instakt.domain.conversion.convert
import hr.kbratko.instakt.domain.getOrRaise
import hr.kbratko.instakt.domain.model.RefreshToken
import hr.kbratko.instakt.domain.model.RefreshToken.Status.Active
import hr.kbratko.instakt.domain.model.RefreshToken.Status.Revoked
import hr.kbratko.instakt.domain.model.User
import hr.kbratko.instakt.domain.persistence.RefreshTokenPersistence
import hr.kbratko.instakt.domain.security.Token
import hr.kbratko.instakt.domain.toKotlinInstant
import java.time.ZoneOffset.UTC
import java.util.UUID
import kotlinx.datetime.toJavaInstant
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.kotlin.datetime.timestampWithTimeZone
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import kotlin.time.Duration
import org.jetbrains.exposed.sql.ReferenceOption.CASCADE as Cascade
import org.jetbrains.exposed.sql.SchemaUtils.create as createIfNotExists

object RefreshTokensTable : UUIDTable("refresh_tokens", "refresh_token_pk") {
    val userId = reference("user_fk", UsersTable, onDelete = Cascade)
    val issuedAt = timestampWithTimeZone("issued_at")
    val expiresAt = timestampWithTimeZone("expires_at")
    val revokedAt = timestampWithTimeZone("revoked_at").nullable()
}

data class RefreshTokenPersistenceConfig(val expiresAfter: Duration)

fun ExposedRefreshTokenPersistence(db: Database, config: RefreshTokenPersistenceConfig) =
    object : RefreshTokenPersistence {
        init {
            transaction {
                createIfNotExists(RefreshTokensTable)
            }
        }

        override suspend fun insert(userId: User.Id): Token.Refresh = ioTransaction(db = db) {
            val id = RefreshTokensTable.insertAndGetId {
                val issuedAt = RoundedInstantProvider.now()

                it[this.userId] = userId.value
                it[this.issuedAt] = issuedAt.toJavaInstant().atOffset(UTC)
                it[expiresAt] = (issuedAt + config.expiresAfter).toJavaInstant().atOffset(UTC)
            }

            Token.Refresh(id.value.toString())
        }

        override suspend fun select(token: Token.Refresh): Option<RefreshToken> = ioTransaction(db = db) {
            token.toUUIDOrNone()
                .flatMap { id ->
                    selectJoiningWithUsers()
                        .where { RefreshTokensTable.id eq id }
                        .singleOrNone()
                        .map { it.convert(ResultRowToRefreshTokenConversion) }
                }
        }

        override suspend fun revoke(token: Token.Refresh): Either<DomainError, RefreshToken> = either {
            ioTransaction(db = db) {
                val id = token.toUUIDOrNone().getOrRaise { InvalidRefreshToken }

                val refreshToken = selectJoiningWithUsers()
                        .where { RefreshTokensTable.id eq id }
                        .singleOrNone()
                        .map { it.convert(ResultRowToRefreshTokenConversion) }
                        .getOrRaise { InvalidRefreshToken }

                when (refreshToken.status) {
                    is Active -> {
                        val now = RoundedInstantProvider.now()

                        RefreshTokensTable.update({ RefreshTokensTable.id eq id }) {
                            it[revokedAt] = now.toJavaInstant().atOffset(UTC)
                        }

                        refreshToken.copy(status = Revoked(now))
                    }

                    is Revoked -> {
                        raise(RefreshTokenAlreadyRevoked)
                    }
                }
            }
        }

        private fun selectJoiningWithUsers() =
            (RefreshTokensTable innerJoin UsersTable)
                .select(
                    RefreshTokensTable.id,
                    RefreshTokensTable.userId,
                    UsersTable.role,
                    RefreshTokensTable.issuedAt,
                    RefreshTokensTable.expiresAt,
                    RefreshTokensTable.revokedAt
                )

        override suspend fun prolong(token: Token.Refresh): Either<DomainError, Token.Refresh> = either {
            ioTransaction(db = db) {
                val id = token.toUUIDOrNone().getOrRaise { InvalidRefreshToken }

                val (status, expiresAt) = RefreshTokensTable
                    .select(
                        RefreshTokensTable.revokedAt,
                        RefreshTokensTable.expiresAt
                    )
                    .where { RefreshTokensTable.id eq id }
                    .singleOrNone()
                    .map {
                        Pair(
                            it.convert(ResultRowToRefreshTokenStatusConversion),
                            it[RefreshTokensTable.expiresAt].toKotlinInstant()
                        )
                    }
                    .getOrRaise { InvalidRefreshToken }

                when (status) {
                    is Active -> {
                        val now = RoundedInstantProvider.now()

                        if (now <= expiresAt)
                            raise(RefreshTokenStillValid)

                        RefreshTokensTable.update({ RefreshTokensTable.id eq id }) {
                            it[this.expiresAt] = (now + config.expiresAfter).toJavaInstant().atOffset(UTC)
                        }

                        token
                    }

                    is Revoked -> {
                        raise(RefreshTokenAlreadyRevoked)
                    }
                }
            }
        }

        override suspend fun revokeExpired(): Int = ioTransaction(db = db) {
            RefreshTokensTable.update({
                RefreshTokensTable.expiresAt lessEq RoundedInstantProvider.now().toJavaInstant().atOffset(UTC) and
                        (RefreshTokensTable.revokedAt.isNull())
            }) {
                it[revokedAt] = RoundedInstantProvider.now().toJavaInstant().atOffset(UTC)
            }
        }

        override suspend fun deleteExpiredFor(duration: Duration): Int = ioTransaction(db = db) {
            RefreshTokensTable.deleteWhere {
                expiresAt lessEq (RoundedInstantProvider.now() - duration).toJavaInstant().atOffset(UTC)
            }
        }

        private fun Token.Refresh.toUUIDOrNone() = catch { UUID.fromString(value) }
    }

typealias ResultRowToRefreshTokenConversionScope = ConversionScope<ResultRow, RefreshToken>

val ResultRowToRefreshTokenConversion = ResultRowToRefreshTokenConversionScope {
    RefreshToken(
        Token.Refresh(this[RefreshTokensTable.id].value.toString()),
        User.Id(this[RefreshTokensTable.userId].value),
        this[UsersTable.role],
        this[RefreshTokensTable.issuedAt].toKotlinInstant(),
        this[RefreshTokensTable.expiresAt].toKotlinInstant(),
        this.convert(ResultRowToRefreshTokenStatusConversion)
    )
}

typealias ResultRowToStatusRefreshTokensConversionScope = ConversionScope<ResultRow, RefreshToken.Status>

val ResultRowToRefreshTokenStatusConversion = ResultRowToStatusRefreshTokensConversionScope {
    this[RefreshTokensTable.revokedAt]
        ?.let { Revoked(it.toKotlinInstant()) }
        ?: Active
}
