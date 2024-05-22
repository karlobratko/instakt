package hr.kbratko.instakt.domain.security.jwt

import arrow.core.Either
import arrow.core.EitherNel
import arrow.core.flatMap
import arrow.core.raise.either
import arrow.core.right
import arrow.core.toEitherNel
import hr.kbratko.instakt.domain.DomainError
import hr.kbratko.instakt.domain.config.RoundedInstantProvider
import hr.kbratko.instakt.domain.conversion.convert
import hr.kbratko.instakt.domain.persistence.RefreshTokenPersistence
import hr.kbratko.instakt.domain.security.Security
import hr.kbratko.instakt.domain.security.SecurityContext
import hr.kbratko.instakt.domain.security.Token.Access
import hr.kbratko.instakt.domain.security.Token.Refresh
import hr.kbratko.instakt.domain.security.TokenCache
import hr.kbratko.instakt.domain.validation.ClaimsAreValid
import hr.kbratko.instakt.domain.validation.TokenIsNotExpired
import hr.kbratko.instakt.domain.validation.validate
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

interface JwtTokenService {
    suspend fun generate(securityContext: SecurityContext): Either<DomainError, Pair<Refresh, Access>>

    suspend fun verify(token: Access): EitherNel<DomainError, SecurityContext>

    suspend fun refresh(token: Refresh): EitherNel<DomainError, Pair<Refresh, Access>>

    suspend fun revoke(token: Refresh): Either<DomainError, Refresh>
}

fun JwtTokenService(
    security: Security,
    algebra: AccessTokens,
    refreshTokenPersistence: RefreshTokenPersistence,
    accessTokenCache: TokenCache<SecurityContext>
) = object : JwtTokenService {
    override suspend fun generate(securityContext: SecurityContext): Either<DomainError, Pair<Refresh, Access>> =
        either {
            val subject = securityContext.userId.convert(UserIdToSubjectConversion)
            val role = securityContext.role.convert(RoleToRoleClaimConversion)
            val audience = Claims.Audience(securityContext.userId.value.toString())
            val issuedAt = RoundedInstantProvider.now()

            with(algebra) {
                val accessToken = Claims(
                    Claims.Issuer(security.issuer),
                    subject,
                    audience,
                    role,
                    issuedAt,
                    issuedAt + security.accessLasting
                ).generate(security.secret).bind()

                coroutineScope {
                    val insertedRefreshToken = async { refreshTokenPersistence.insert(securityContext.userId) }

                    launch { accessTokenCache.put(accessToken, securityContext) }

                    insertedRefreshToken.await() to accessToken
                }
            }
        }

    override suspend fun verify(token: Access): EitherNel<DomainError, SecurityContext> =
        accessTokenCache.get(token)
            .fold(
                ifEmpty = {
                    with(algebra) {
                        token.extract(security.secret).toEitherNel()
                            .flatMap { claims ->
                                claims.validate(ClaimsAreValid)
                            }
                            .flatMap { claims ->
                                either {
                                    SecurityContext(
                                        claims.subject.convert(SubjectToUserIdConversion),
                                        claims.role.convert(RoleClaimToRoleConversion)
                                    )
                                }
                            }
                            .onRight { auth -> accessTokenCache.put(token, auth) }
                    }
                },
                ifSome = { auth -> auth.right() }
            )

    override suspend fun refresh(token: Refresh): EitherNel<DomainError, Pair<Refresh, Access>> =
        refreshTokenPersistence.revoke(token).toEitherNel()
            .flatMap { entity ->
                entity.expiresAt
                    .validate(TokenIsNotExpired)
                    .map { entity }
            }
            .flatMap { entity -> generate(SecurityContext(entity.userId, entity.role)).toEitherNel() }

    override suspend fun revoke(token: Refresh): Either<DomainError, Refresh> =
        refreshTokenPersistence.revoke(token).map { it.token }
}