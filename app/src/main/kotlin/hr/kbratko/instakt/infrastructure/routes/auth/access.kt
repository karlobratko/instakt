package hr.kbratko.instakt.infrastructure.routes.auth

import arrow.core.raise.either
import hr.kbratko.instakt.domain.DomainError
import hr.kbratko.instakt.domain.utility.eitherNel
import hr.kbratko.instakt.domain.model.User
import hr.kbratko.instakt.domain.persistence.UserPersistence
import hr.kbratko.instakt.domain.security.SecurityContext
import hr.kbratko.instakt.domain.security.Token
import hr.kbratko.instakt.domain.security.jwt.JwtTokenService
import hr.kbratko.instakt.domain.validation.PasswordIsValid
import hr.kbratko.instakt.domain.validation.UsernameIsValid
import hr.kbratko.instakt.domain.validation.validate
import hr.kbratko.instakt.infrastructure.plugins.restrictedRateLimit
import hr.kbratko.instakt.infrastructure.routes.foldValidation
import hr.kbratko.instakt.infrastructure.routes.toResponse
import io.ktor.http.HttpStatusCode
import io.ktor.resources.Resource
import io.ktor.server.application.call
import io.ktor.server.plugins.requestvalidation.RequestValidationConfig
import io.ktor.server.resources.post
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject

@Resource("/access")
data class Access(val parent: Auth = Auth()) {
    @Resource("/acquire")
    data class Acquire(val parent: Access = Access()) {
        @Serializable data class Body(val username: String, val password: String)
    }

    @Resource("/refresh")
    data class Refresh(val parent: Access = Access()) {
        @Serializable data class Body(val refreshToken: String)
    }

    @Resource("/revoke")
    data class Revoke(val parent: Access = Access()) {
        @Serializable data class Body(val refreshToken: String)
    }

    @Serializable data class Response(val accessToken: Token.Access, val refreshToken: Token.Refresh)
}

fun RequestValidationConfig.accessValidation() {
    validate<Access.Acquire.Body> { request ->
        validate(request) {
            with { it.username.validate(UsernameIsValid) }
            with { it.password.validate(PasswordIsValid) }
        }.foldValidation()
    }
}

fun Route.access() {
    val userPersistence by inject<UserPersistence>()
    val jwtTokenService by inject<JwtTokenService>()

    restrictedRateLimit {
        post<Access.Acquire, Access.Acquire.Body> { _, body ->
            either {
                val user =
                    userPersistence.select(User.Username(body.username), User.Password(body.password)).bind()

                val (refreshToken, accessToken) = jwtTokenService.generate(SecurityContext(user.id, user.role)).bind()

                Access.Response(accessToken, refreshToken)
            }.toResponse(HttpStatusCode.OK).let { call.respond(it.code, it) }
        }

        post<Access.Refresh, Access.Refresh.Body> { _, body ->
            eitherNel {
                val (refreshToken, accessToken) = jwtTokenService.refresh(Token.Refresh(body.refreshToken))
                    .bind()

                Access.Response(accessToken, refreshToken)
            }.toResponse(HttpStatusCode.OK).let { call.respond(it.code, it) }
        }

        post<Access.Revoke, Access.Revoke.Body> { _, body ->
            either<DomainError, Unit> {
                jwtTokenService.revoke(Token.Refresh(body.refreshToken)).bind()
            }.toResponse(HttpStatusCode.OK).let { call.respond(it.code, it) }
        }
    }
}