package hr.kbratko.instakt.infrastructure.routes.resources

import hr.kbratko.instakt.domain.model.Plan
import hr.kbratko.instakt.infrastructure.plugins.permissiveRateLimit
import hr.kbratko.instakt.infrastructure.routes.Response
import io.ktor.http.HttpStatusCode
import io.ktor.resources.Resource
import io.ktor.server.application.call
import io.ktor.server.resources.get
import io.ktor.server.response.respond
import io.ktor.server.routing.Route

@Resource("/plans")
data class Plans(val parent: Resources = Resources())

fun Route.plans() {

    permissiveRateLimit {
        get<Plans> {
            val metadata = Plan.entries.toList()
            Response.Success(metadata, HttpStatusCode.OK)
                .let { call.respond(it.code, it) }
        }
    }
}