package hr.kbratko.instakt.infrastructure.routes.account.profile

import arrow.core.raise.either
import hr.kbratko.instakt.domain.model.SocialMediaLink
import hr.kbratko.instakt.domain.persistence.SocialMediaLinkPersistence
import hr.kbratko.instakt.infrastructure.ktor.principal
import hr.kbratko.instakt.infrastructure.plugins.jwt
import hr.kbratko.instakt.infrastructure.plugins.permissiveRateLimit
import hr.kbratko.instakt.infrastructure.plugins.restrictedRateLimit
import hr.kbratko.instakt.infrastructure.routes.Response
import hr.kbratko.instakt.infrastructure.routes.toResponse
import hr.kbratko.instakt.infrastructure.security.UserPrincipal
import io.ktor.http.HttpStatusCode
import io.ktor.resources.Resource
import io.ktor.server.application.call
import io.ktor.server.resources.delete
import io.ktor.server.resources.get
import io.ktor.server.resources.post
import io.ktor.server.resources.put
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject

@Resource("/social")
data class Social(val parent: Profile = Profile()) {
    @Serializable data class Body(
        val platform: String,
        val url: String
    )

    @Resource("/{id}")
    data class Id(val parent: Social = Social(), val id: Long) {
        @Serializable data class Body(
            val platform: String,
            val url: String
        )
    }
}

fun Route.social() {
    val socialMediaLinkPersistence by inject<SocialMediaLinkPersistence>()

    permissiveRateLimit {
        jwt {
            get<Social> {
                val principal = call.principal<UserPrincipal>()
                val links = socialMediaLinkPersistence.select(principal.id)

                Response.Success(links, HttpStatusCode.OK)
                    .let { call.respond(it.code, it) }
            }
        }
    }

    restrictedRateLimit {
        jwt {
            post<Social, Social.Body> { _, body ->
                either {
                    val principal = call.principal<UserPrincipal>()

                    socialMediaLinkPersistence.insert(
                        SocialMediaLink.New(
                            principal.id,
                            SocialMediaLink.Platform(body.platform),
                            SocialMediaLink.Url(body.url)
                        )
                    ).bind()
                }.toResponse(HttpStatusCode.Created).let { call.respond(it.code, it) }
            }

            put<Social.Id, Social.Id.Body> { resource, body ->
                either {
                    socialMediaLinkPersistence.update(
                        SocialMediaLink.Edit(
                            SocialMediaLink.Id(resource.id),
                            SocialMediaLink.Platform(body.platform),
                            SocialMediaLink.Url(body.url)
                        )
                    ).bind()
                }.toResponse(HttpStatusCode.OK).let { call.respond(it.code, it) }
            }

            delete<Social.Id> { resource ->
                either {
                    socialMediaLinkPersistence.delete(
                        SocialMediaLink.Id(resource.id)
                    ).bind()
                }.toResponse(HttpStatusCode.OK).let { call.respond(it.code, it) }
            }
        }
    }
}