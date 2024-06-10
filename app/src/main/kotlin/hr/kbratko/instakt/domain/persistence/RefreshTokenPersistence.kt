package hr.kbratko.instakt.domain.persistence

import arrow.core.Either
import hr.kbratko.instakt.domain.DomainError
import hr.kbratko.instakt.domain.model.RefreshToken
import hr.kbratko.instakt.domain.model.User
import hr.kbratko.instakt.domain.security.Token
import kotlin.time.Duration

interface RefreshTokenPersistence {
    suspend fun insert(userId: User.Id): Token.Refresh

    suspend fun revoke(token: Token.Refresh): Either<DomainError, RefreshToken>

    suspend fun prolong(token: Token.Refresh): Either<DomainError, Token.Refresh>

    suspend fun revokeExpired(): Int

    suspend fun deleteExpiredFor(duration: Duration): Int
}
