package hr.kbratko.instakt.infrastructure.routes.auth

import arrow.core.toEitherNel
import hr.kbratko.instakt.domain.DomainError
import hr.kbratko.instakt.domain.eitherNel
import hr.kbratko.instakt.domain.model.User
import hr.kbratko.instakt.domain.persistence.UserPersistence
import hr.kbratko.instakt.domain.security.SecurityContext
import hr.kbratko.instakt.domain.security.Token
import hr.kbratko.instakt.domain.security.jwt.JwtTokenService
import hr.kbratko.instakt.infrastructure.plugins.jwt
import hr.kbratko.instakt.infrastructure.plugins.restrictedRateLimit
import hr.kbratko.instakt.infrastructure.routes.toResponse
import io.ktor.http.HttpStatusCode
import io.ktor.resources.Resource
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.routing.Route
import io.ktor.server.resources.post
import io.ktor.server.response.respond
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject

@Resource("/access")
data class Access(val parent: Auth = Auth()) {
    @Resource("/acquire")
    data class Acquire(val parent: Access = Access())

    @Resource("/refresh")
    data class Refresh(val parent: Access = Access())

    @Resource("/revoke")
    data class Revoke(val parent: Access = Access())
}

fun Route.access() {
    val userPersistence by inject<UserPersistence>()
    val jwtTokenService by inject<JwtTokenService>()

    restrictedRateLimit {
        post<Access.Acquire> {
            eitherNel {
                val content = call.receive<AcquireAccess>()

                val user = userPersistence.select(User.Username(content.username), User.Password(content.password))
                    .toEitherNel().bind()

                val (refreshToken, accessToken) = jwtTokenService.generate(SecurityContext(user.id, user.role))
                    .toEitherNel().bind()

                GrantAccess(accessToken.value, refreshToken.value)
            }.toResponse(HttpStatusCode.OK).let { call.respond(it.code, it) }
        }

        post<Access.Refresh> {
            eitherNel {
                val content = call.receive<RefreshAccess>()

                val (refreshToken, accessToken) = jwtTokenService.refresh(Token.Refresh(content.refreshToken))
                    .bind()

                GrantAccess(accessToken.value, refreshToken.value)
            }.toResponse(HttpStatusCode.OK).let { call.respond(it.code, it) }
        }

        post<Access.Revoke> {
            eitherNel<DomainError, Unit> {
                val content = call.receive<RevokeAccess>()

                jwtTokenService.revoke(Token.Refresh(content.refreshToken)).toEitherNel().bind()
            }.toResponse(HttpStatusCode.OK).let { call.respond(it.code, it) }
        }
    }
}

@Serializable data class AcquireAccess(val username: String, val password: String)

@Serializable data class GrantAccess(val accessToken: String, val refreshToken: String)

@Serializable data class RefreshAccess(val refreshToken: String)

@Serializable data class RevokeAccess(val refreshToken: String)