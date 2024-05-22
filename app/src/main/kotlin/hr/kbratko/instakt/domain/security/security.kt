package hr.kbratko.instakt.domain.security

import hr.kbratko.instakt.domain.model.User
import kotlin.time.Duration

data class Security(
    val issuer: String,
    val secret: String,
    val accessLasting: Duration
)

data class SecurityContext(val userId: User.Id, val role: User.Role)
