package hr.kbratko.instakt.infrastructure.routes.auth

import hr.kbratko.instakt.infrastructure.routes.Api
import io.ktor.resources.Resource
import io.ktor.server.routing.Route

@Resource("/auth")
data class Auth(val parent: Api = Api)

fun Route.auth() {
    register()
    access()
}