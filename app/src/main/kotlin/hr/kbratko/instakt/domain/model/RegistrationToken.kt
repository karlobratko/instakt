package hr.kbratko.instakt.domain.model

import hr.kbratko.instakt.domain.model.RegistrationToken.Status.Unconfirmed
import hr.kbratko.instakt.domain.security.Token
import kotlinx.datetime.Instant

data class RegistrationToken(
    val token: Token.Register,
    val createdAt: Instant,
    val expiresAt: Instant,
    val status: Status = Unconfirmed
) {
    sealed interface Status {
        @JvmInline value class Confirmed(val value: Instant) : Status

        data object Unconfirmed : Status
    }
}
