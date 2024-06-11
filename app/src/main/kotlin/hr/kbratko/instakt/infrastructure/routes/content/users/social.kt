package hr.kbratko.instakt.infrastructure.routes.content.users

import hr.kbratko.instakt.domain.model.User
import hr.kbratko.instakt.domain.persistence.SocialMediaLinkPersistence
import hr.kbratko.instakt.infrastructure.plugins.permissiveRateLimit
import hr.kbratko.instakt.infrastructure.routes.Response
import io.ktor.http.HttpStatusCode
import io.ktor.resources.Resource
import io.ktor.server.application.call
import io.ktor.server.resources.get
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import org.koin.ktor.ext.inject

@Resource("/social")
data class Social(val parent: Users)

fun Route.social() {
    val socialMediaLinkPersistence by inject<SocialMediaLinkPersistence>()

    permissiveRateLimit {
        get<Social> { resource ->
            val links = socialMediaLinkPersistence.select(User.Id(resource.parent.id))

            Response.Success(links, HttpStatusCode.OK)
                .let { call.respond(it.code, it) }
        }
    }
}