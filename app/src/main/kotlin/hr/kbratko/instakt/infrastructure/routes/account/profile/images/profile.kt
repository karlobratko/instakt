package hr.kbratko.instakt.infrastructure.routes.account.profile.images

import arrow.core.nel
import arrow.core.raise.either
import arrow.fx.coroutines.autoCloseable
import arrow.fx.coroutines.resourceScope
import hr.kbratko.instakt.domain.DbError.ProfilePictureMetadataNotFound
import hr.kbratko.instakt.domain.DomainError
import hr.kbratko.instakt.domain.RequestError.RequestCouldNotBeProcessed
import hr.kbratko.instakt.domain.content.ContentService
import hr.kbratko.instakt.domain.eitherNel
import hr.kbratko.instakt.domain.persistence.ContentMetadataPersistence
import hr.kbratko.instakt.infrastructure.ktor.principal
import hr.kbratko.instakt.infrastructure.plugins.jwt
import hr.kbratko.instakt.infrastructure.plugins.permissiveRateLimit
import hr.kbratko.instakt.infrastructure.plugins.restrictedRateLimit
import hr.kbratko.instakt.infrastructure.routes.toResponse
import hr.kbratko.instakt.infrastructure.security.UserPrincipal
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import io.ktor.resources.Resource
import io.ktor.server.application.call
import io.ktor.server.request.receiveMultipart
import io.ktor.server.resources.delete
import io.ktor.server.resources.get
import io.ktor.server.resources.post
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import java.io.InputStream
import org.koin.ktor.ext.inject

@Resource("/profile")
data class Profile(val parent: Images = Images())

fun Route.profile() {
    val contentMetadataPersistence by inject<ContentMetadataPersistence>()
    val contentService by inject<ContentService>()

    permissiveRateLimit {
        jwt {
            get<Profile> {
                either {
                    val principal = call.principal<UserPrincipal>()
                    contentMetadataPersistence.selectProfile(principal.id)
                        .toEither { ProfilePictureMetadataNotFound }.bind()
                }.toResponse(HttpStatusCode.OK).let { call.respond(it.code, it) }
            }
        }
    }

    restrictedRateLimit {
        jwt {
            post<Profile> {
                resourceScope {
                    eitherNel {
                        val principal = call.principal<UserPrincipal>()
                        val multipart = call.receiveMultipart()

                        lateinit var imageStream: InputStream
                        multipart.forEachPart {
                            val part = install({ it }, { value, _ -> value.dispose() })
                            when {
                                part is PartData.FileItem && part.name == "content" -> {
                                    imageStream = autoCloseable { part.streamProvider().buffered() }
                                }

                                else -> {
                                    raise(RequestCouldNotBeProcessed.nel())
                                }
                            }
                        }

                        contentService.uploadProfilePhoto(principal.id, imageStream).bind()
                    }.toResponse(HttpStatusCode.OK).let { call.respond(it.code, it) }
                }
            }

            delete<Profile> {
                either<DomainError, Unit> {
                    val principal = call.principal<UserPrincipal>()
                    contentService.deleteProfilePhoto(principal.id).bind()
                }.toResponse(HttpStatusCode.OK).let { call.respond(it.code, it) }
            }
        }
    }
}