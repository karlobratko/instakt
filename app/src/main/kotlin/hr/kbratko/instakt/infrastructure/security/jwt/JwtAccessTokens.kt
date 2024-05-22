package hr.kbratko.instakt.infrastructure.security.jwt

import arrow.core.Either
import arrow.core.Either.Companion.catch
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm.HMAC512
import hr.kbratko.instakt.domain.SecurityError
import hr.kbratko.instakt.domain.SecurityError.ClaimsExtractionError
import hr.kbratko.instakt.domain.SecurityError.TokenGenerationFailed
import hr.kbratko.instakt.domain.security.Token.Access
import hr.kbratko.instakt.domain.security.jwt.AccessTokens
import hr.kbratko.instakt.domain.security.jwt.Claims
import hr.kbratko.instakt.domain.security.jwt.Claims.Audience
import hr.kbratko.instakt.domain.security.jwt.Claims.Issuer
import hr.kbratko.instakt.domain.security.jwt.Claims.Role
import hr.kbratko.instakt.domain.security.jwt.Claims.Subject
import kotlinx.datetime.toJavaInstant
import kotlinx.datetime.toKotlinInstant

var JwtAccessTokens = object : AccessTokens {
    override suspend fun Claims.generate(secret: String): Either<SecurityError, Access> =
        catch {
            JWT.create()
                .withSubject(subject.value)
                .withIssuer(issuer.value)
                .withAudience(audience.value)
                .withClaim("role", role.value)
                .withIssuedAt(issuedAt.toJavaInstant())
                .withExpiresAt(expiresAt.toJavaInstant())
                .sign(HMAC512(secret))
        }.map { Access(it) }.mapLeft { TokenGenerationFailed }

    override suspend fun Access.extract(secret: String): Either<SecurityError, Claims> =
        catch {
            val decoded = JWT.require(HMAC512(secret)).build().verify(value)

            Claims(
                Issuer(decoded.issuer!!),
                Subject(decoded.subject!!),
                Audience(decoded.audience[0]!!),
                Role(decoded.claims["role"]!!.asString()),
                decoded.issuedAtAsInstant!!.toKotlinInstant(),
                decoded.expiresAtAsInstant!!.toKotlinInstant()
            )
        }.mapLeft { ClaimsExtractionError }
}
