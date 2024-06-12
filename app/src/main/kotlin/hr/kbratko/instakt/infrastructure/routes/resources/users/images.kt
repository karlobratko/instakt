package hr.kbratko.instakt.infrastructure.routes.resources.users

import arrow.core.raise.either
import hr.kbratko.instakt.domain.DbError.ProfilePictureMetadataNotFound
import hr.kbratko.instakt.domain.model.User
import hr.kbratko.instakt.domain.persistence.ContentMetadataPersistence
import hr.kbratko.instakt.infrastructure.plugins.permissiveRateLimit
import hr.kbratko.instakt.infrastructure.routes.Response
import hr.kbratko.instakt.infrastructure.routes.toResponse
import io.ktor.http.HttpStatusCode
import io.ktor.resources.Resource
import io.ktor.server.application.call
import io.ktor.server.resources.get
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import org.koin.ktor.ext.inject

@Resource("/images")
data class Images(val parent: Users) {
    @Resource("/profile")
    data class Profile(val parent: Images)
}

fun Route.images() {
    val contentMetadataPersistence by inject<ContentMetadataPersistence>()

    permissiveRateLimit {
        get<Images> { resource ->
            val metadata = contentMetadataPersistence.selectNonProfile(User.Id(resource.parent.id))
            Response.Success(metadata, HttpStatusCode.OK)
                .let { call.respond(it.code, it) }
        }

        get<Images.Profile> { resource ->
            either {
                contentMetadataPersistence.selectProfile(User.Id(resource.parent.parent.id))
                    .toEither { ProfilePictureMetadataNotFound }.bind()
            }.toResponse(HttpStatusCode.OK).let { call.respond(it.code, it) }
        }
    }
}