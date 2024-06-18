package hr.kbratko.instakt.infrastructure.routes.account.profile.images

import arrow.core.nel
import arrow.core.raise.either
import arrow.fx.coroutines.autoCloseable
import arrow.fx.coroutines.resourceScope
import hr.kbratko.instakt.domain.DomainError
import hr.kbratko.instakt.domain.RequestError.RequestCouldNotBeProcessed
import hr.kbratko.instakt.domain.content.ContentService
import hr.kbratko.instakt.domain.content.ContentService.Metadata
import hr.kbratko.instakt.domain.utility.eitherNel
import hr.kbratko.instakt.domain.model.ContentMetadata
import hr.kbratko.instakt.domain.persistence.ContentMetadataPersistence
import hr.kbratko.instakt.domain.validation.ContentDescriptionIsValid
import hr.kbratko.instakt.domain.validation.ContentTagsAreValid
import hr.kbratko.instakt.domain.validation.validate
import hr.kbratko.instakt.infrastructure.ktor.principal
import hr.kbratko.instakt.infrastructure.logging.ActionLogger
import hr.kbratko.instakt.infrastructure.plugins.jwt
import hr.kbratko.instakt.infrastructure.plugins.permissiveRateLimit
import hr.kbratko.instakt.infrastructure.plugins.restrictedRateLimit
import hr.kbratko.instakt.infrastructure.routes.Response
import hr.kbratko.instakt.infrastructure.routes.account.profile.UserProfile
import hr.kbratko.instakt.infrastructure.routes.foldValidation
import hr.kbratko.instakt.infrastructure.routes.toResponse
import hr.kbratko.instakt.infrastructure.security.UserPrincipal
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import io.ktor.resources.Resource
import io.ktor.server.application.call
import io.ktor.server.plugins.requestvalidation.RequestValidationConfig
import io.ktor.server.request.receiveMultipart
import io.ktor.server.resources.delete
import io.ktor.server.resources.get
import io.ktor.server.resources.post
import io.ktor.server.resources.put
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import java.io.InputStream
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.koin.ktor.ext.inject

@Resource("/images")
data class Images(val parent: UserProfile = UserProfile()) {
    @Serializable
    data class Body(
        val description: String,
        val tags: List<String>
    )

    @Resource("/{id}")
    data class Id(val parent: Images = Images(), val id: String) {
        @Serializable
        data class Body(
            val description: String,
            val tags: List<String>
        )
    }
}

fun RequestValidationConfig.profileImagesValidation() {
    validate<Images.Body> { request ->
        validate(request) {
            with { it.description.validate(ContentDescriptionIsValid) }
            with { it.tags.validate(ContentTagsAreValid) }
        }.foldValidation()
    }

    validate<Images.Id.Body> { request ->
        validate(request) {
            with { it.description.validate(ContentDescriptionIsValid) }
            with { it.tags.validate(ContentTagsAreValid) }
        }.foldValidation()
    }
}

fun Route.images() {
    val contentMetadataPersistence by inject<ContentMetadataPersistence>()
    val contentService by inject<ContentService>()
    val actionLogger by inject<ActionLogger>()

    profile()

    permissiveRateLimit {
        jwt {
            get<Images> {
                val principal = call.principal<UserPrincipal>()
                val metadata = contentMetadataPersistence.selectNonProfile(principal.id)
                Response.Success(metadata, HttpStatusCode.OK)
                    .let { call.respond(it.code, it) }
            }
        }
    }

    restrictedRateLimit {
        jwt {
            post<Images> {
                resourceScope {
                    eitherNel {
                        val principal = call.principal<UserPrincipal>()
                        val multipart = call.receiveMultipart()

                        lateinit var imageStream: InputStream
                        lateinit var metadata: Images.Body
                        multipart.forEachPart {
                            val part = install({ it }, { value, _ -> value.dispose() })
                            when {
                                part is PartData.FileItem && part.name == "content" -> {
                                    imageStream = autoCloseable { part.streamProvider().buffered() }
                                }

                                part is PartData.FormItem && part.name == "metadata" -> {
                                    metadata = Json.decodeFromString<Images.Body>(part.value)
                                }

                                else -> {
                                    raise(RequestCouldNotBeProcessed.nel())
                                }
                            }
                        }

                        contentService.upload(
                            principal.id,
                            imageStream,
                            Metadata(
                                ContentMetadata.Description(metadata.description),
                                metadata.tags.map { ContentMetadata.Tag(it) }
                            )
                        ).bind().also { actionLogger.logContentCreation(it.user.id) }
                    }.toResponse(HttpStatusCode.OK).let { call.respond(it.code, it) }
                }
            }

            put<Images.Id, Images.Id.Body> { resource, body ->
                either {
                    val principal = call.principal<UserPrincipal>()
                    contentMetadataPersistence.update(
                        ContentMetadata.Edit(
                            ContentMetadata.Id(resource.id),
                            principal.id,
                            ContentMetadata.Description(body.description),
                            body.tags.map { ContentMetadata.Tag(it) }
                        )
                    ).bind().also { actionLogger.logContentUpdate(it.user.id) }
                }.toResponse(HttpStatusCode.OK).let { call.respond(it.code, it) }
            }

            delete<Images.Id> { resource ->
                either<DomainError, Unit> {
                    val principal = call.principal<UserPrincipal>()
                    val contentMetadataId = ContentMetadata.Id(resource.id)
                    contentService.delete(principal.id, contentMetadataId).bind()

                    actionLogger.logContentDeletion(principal.id)
                }.toResponse(HttpStatusCode.OK).let { call.respond(it.code, it) }
            }
        }
    }
}