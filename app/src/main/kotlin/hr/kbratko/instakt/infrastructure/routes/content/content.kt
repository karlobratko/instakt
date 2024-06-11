package hr.kbratko.instakt.infrastructure.routes.content

import hr.kbratko.instakt.infrastructure.routes.Api
import hr.kbratko.instakt.infrastructure.routes.content.users.users
import io.ktor.resources.Resource
import io.ktor.server.routing.Route

@Resource("/content")
data class Content(val parent: Api = Api)

fun Route.content() {
    users()
}