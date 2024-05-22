package hr.kbratko.instakt.domain.security.jwt

import kotlinx.datetime.Instant

data class Claims(
    val issuer: Issuer,
    val subject: Subject,
    val audience: Audience,
    val role: Role,
    val issuedAt: Instant,
    val expiresAt: Instant,
) {
    @JvmInline value class Issuer(val value: String)

    @JvmInline value class Subject(val value: String)

    @JvmInline value class Audience(val value: String)

    @JvmInline value class Role(val value: String)
}
