package hr.kbratko.instakt.infrastructure.plugins

import hr.kbratko.instakt.domain.model.User
import hr.kbratko.instakt.domain.security.Token
import hr.kbratko.instakt.domain.security.jwt.JwtTokenService
import hr.kbratko.instakt.infrastructure.ktor.principal
import hr.kbratko.instakt.infrastructure.security.AuthenticationException
import hr.kbratko.instakt.infrastructure.security.UserPrincipal
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.AuthenticationStrategy.Required
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.bearer
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

fun Application.configureSecurity() {
    val jwtTokenService by inject<JwtTokenService>()

    install(Authentication) {
        bearer("jwt") {
            authenticate { credential ->
                jwtTokenService.verify(Token.Access(credential.token))
                    .fold(
                        ifLeft = { throw AuthenticationException(it) },
                        ifRight = { (id, role) ->
                            UserPrincipal(id, role)
                        }
                    )
            }
        }
    }
}

fun Route.jwt(build: Route.() -> Unit) =
    authenticate("jwt", strategy = Required) {
        build()
    }

fun Route.admin(build: Route.() -> Unit) = authorize(listOf(User.Role.Admin), build)

fun Route.authorize(vararg allowedRoles: User.Role, build: Route.() -> Unit) = authorize(allowedRoles.toList(), build)

fun Route.authorize(allowedRoles: List<User.Role>, build: Route.() -> Unit): Route {
    return route("") {
        intercept(ApplicationCallPipeline.Call) {
            val principal = call.principal<UserPrincipal>()
            println(principal.role)
            println(allowedRoles)
            if (principal.role !in allowedRoles) {
                call.respond(HttpStatusCode.Forbidden)
                finish()
            }
        }
        build()
    }
}