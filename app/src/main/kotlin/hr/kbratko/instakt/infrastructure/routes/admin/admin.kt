package hr.kbratko.instakt.infrastructure.routes.admin

import hr.kbratko.instakt.infrastructure.routes.Api
import io.ktor.resources.Resource
import io.ktor.server.routing.Route

@Resource("/admin")
data class Admin(val parent: Api = Api)

fun Route.admin() {
    logs()
}