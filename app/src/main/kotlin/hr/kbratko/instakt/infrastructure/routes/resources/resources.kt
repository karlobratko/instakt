package hr.kbratko.instakt.infrastructure.routes.resources

import hr.kbratko.instakt.infrastructure.routes.Api
import hr.kbratko.instakt.infrastructure.routes.resources.users.users
import io.ktor.resources.Resource
import io.ktor.server.routing.Route

@Resource("/resources")
data class Resources(val parent: Api = Api)

fun Route.resources() {
    users()
    images()
    plans()
}