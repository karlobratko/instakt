@file:UseSerializers(
    OptionSerializer::class,
)

package hr.kbratko.instakt.infrastructure.routes.resources.images

import arrow.core.Option
import arrow.core.right
import arrow.core.serialization.OptionSerializer
import arrow.core.toNonEmptyListOrNone
import hr.kbratko.instakt.domain.DbError.ContentNotFound
import hr.kbratko.instakt.domain.InstantClosedRange
import hr.kbratko.instakt.domain.model.Content
import hr.kbratko.instakt.domain.model.ContentMetadata
import hr.kbratko.instakt.domain.model.User
import hr.kbratko.instakt.domain.persistence.ContentMetadataPersistence
import hr.kbratko.instakt.domain.persistence.ContentPersistence
import hr.kbratko.instakt.domain.persistence.pagination.Page
import hr.kbratko.instakt.domain.persistence.pagination.Sort
import hr.kbratko.instakt.domain.validation.ContentSortTermIsValid
import hr.kbratko.instakt.domain.validation.PaginationIsValid
import hr.kbratko.instakt.domain.validation.UploadRangeIsValid
import hr.kbratko.instakt.domain.validation.validate
import hr.kbratko.instakt.infrastructure.plugins.permissiveRateLimit
import hr.kbratko.instakt.infrastructure.routes.Response
import hr.kbratko.instakt.infrastructure.routes.foldValidation
import hr.kbratko.instakt.infrastructure.routes.resources.Resources
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.resources.Resource
import io.ktor.server.application.call
import io.ktor.server.plugins.requestvalidation.RequestValidationConfig
import io.ktor.server.resources.get
import io.ktor.server.resources.post
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.Route
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import org.koin.ktor.ext.inject

@Resource("/images")
data class Images(val parent: Resources = Resources()) {
    @Serializable data class Body(
        val filter: Filter,
        val page: Page,
        val sort: Option<Sort>
    ) {
        @Serializable data class Filter(
            val username: Option<String>,
            val description: Option<String>,
            val uploadedBetween: Option<InstantClosedRange>,
            val tags: Option<List<String>>
        )
    }

    @Resource("/{id...}")
    data class Id(val parent: Images = Images(), val id: List<String>)
}

fun RequestValidationConfig.imagesValidation() {
    validate<Images.Body> { request ->
        validate(request) {
            with { request ->
                request.filter.uploadedBetween
                    .fold(
                        ifEmpty = { request.right() },
                        ifSome = {
                            it.validate(UploadRangeIsValid)
                        }
                    )
            }
            with { request ->
                request.sort
                    .fold(
                        ifEmpty = { request.right() },
                        ifSome = {
                            it.by.validate(ContentSortTermIsValid)
                        }
                    )
            }
            with { it.page.validate(PaginationIsValid(50)) }
        }.foldValidation()
    }
}

fun Route.images() {
    val contentPersistence by inject<ContentPersistence>()
    val contentMetadataPersistence by inject<ContentMetadataPersistence>()

    permissiveRateLimit {
        get<Images.Id> { resource ->
            contentPersistence.download(Content.Id(resource.id.joinToString("/", prefix = "/")))
                .toEither { ContentNotFound }
                .onRight {
                    call.respondBytes(it.value, ContentType.parse(it.type.value))
                }
                .onLeft {
                    call.respond(HttpStatusCode.NotFound)
                }
        }

        post<Images, Images.Body> { _, body ->
            val metadata = contentMetadataPersistence.select(
                body.filter.run {
                    ContentMetadata.Filter(
                        username.map { User.Username(it) },
                        description.map { ContentMetadata.Description(it) },
                        uploadedBetween,
                        tags.flatMap { it.toNonEmptyListOrNone() }
                            .map { nel -> nel.map { ContentMetadata.Tag(it) } }
                    )
                },
                body.page,
                body.sort
            )

            Response.Success(metadata, HttpStatusCode.OK)
                .let { call.respond(it.code, it) }
        }
    }
}