package hr.kbratko.instakt.domain.model

import hr.kbratko.instakt.domain.security.Token
import kotlinx.datetime.Instant

data class RefreshToken(
    val token: Token.Refresh,
    val userId: User.Id,
    val role: User.Role,
    val issuedAt: Instant,
    val expiresAt: Instant,
    val status: Status
) {
    sealed interface Status {
        data object Active : Status

        @JvmInline value class Revoked(val value: Instant) : Status
    }
}
