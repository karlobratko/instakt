package hr.kbratko.instakt.infrastructure.routes.resources.images

import hr.kbratko.instakt.domain.DbError.ContentNotFound
import hr.kbratko.instakt.domain.model.Content
import hr.kbratko.instakt.domain.persistence.ContentPersistence
import hr.kbratko.instakt.infrastructure.plugins.permissiveRateLimit
import hr.kbratko.instakt.infrastructure.routes.resources.Resources
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.resources.Resource
import io.ktor.server.application.call
import io.ktor.server.resources.get
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.Route
import org.koin.ktor.ext.inject

@Resource("/images/{id...}")
data class Images(val parent: Resources = Resources(), val id: List<String>)

fun Route.images() {
    val contentPersistence by inject<ContentPersistence>()

    permissiveRateLimit {
        get<Images> { resource ->
            contentPersistence.download(Content.Id(resource.id.joinToString("/")))
                .toEither { ContentNotFound }
                .onRight {
                    call.respondBytes(it.value, ContentType.parse(it.type.value))
                }
                .onLeft {
                    call.respond(HttpStatusCode.NotFound)
                }
        }
    }
}