package hr.kbratko.instakt.infrastructure.routes.account

import arrow.core.raise.either
import hr.kbratko.instakt.domain.DbError.UserNotFound
import hr.kbratko.instakt.domain.model.User
import hr.kbratko.instakt.domain.persistence.UserPersistence
import hr.kbratko.instakt.infrastructure.ktor.principal
import hr.kbratko.instakt.infrastructure.plugins.jwt
import hr.kbratko.instakt.infrastructure.plugins.permissiveRateLimit
import hr.kbratko.instakt.infrastructure.plugins.restrictedRateLimit
import hr.kbratko.instakt.infrastructure.routes.toResponse
import hr.kbratko.instakt.infrastructure.security.UserPrincipal
import io.ktor.http.HttpStatusCode
import io.ktor.resources.Resource
import io.ktor.server.application.call
import io.ktor.server.resources.get
import io.ktor.server.resources.post
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject

@Resource("/profile")
data class Profile(val parent: Account = Account()) {
    @Serializable data class Body(
        val firstName: String,
        val lastName: String,
        val bio: String
    )

    @Resource("{id}")
    data class Id(val parent: Profile = Profile(), val id: Long)

    @Resource("/password")
    data class Password(val parent: Profile = Profile()) {
        @Serializable data class Body(
            val oldPassword: String,
            val newPassword: String
        )
    }

    @Resource("/social")
    data class Social(val parent: Profile = Profile())

    @Resource("/picture")
    data class Picture(val parent: Profile = Profile())
}

fun Route.profile() {
    val userPersistence by inject<UserPersistence>()

    permissiveRateLimit {
        get<Profile.Id> { resource ->
            either {
                userPersistence.selectProfile(User.Id(resource.id)).toEither { UserNotFound }.bind()
            }.toResponse(HttpStatusCode.OK).let { call.respond(it.code, it) }
        }
    }

    restrictedRateLimit {
        jwt {
            post<Profile, Profile.Body> { _, body ->
                either {
                    val principal = call.principal<UserPrincipal>()

                    userPersistence.update(
                        User.Edit(
                            principal.id,
                            User.FirstName(body.bio),
                            User.LastName(body.bio),
                            User.Bio(body.bio)
                        )
                    ).bind()
                }.toResponse(HttpStatusCode.OK).let { call.respond(it.code, it) }
            }

            post<Profile, Profile.Password.Body> { _, body ->
                either {
                    val principal = call.principal<UserPrincipal>()

                    userPersistence.update(
                        User.ChangePassword(
                            principal.id,
                            User.Password(body.oldPassword),
                            User.Password(body.newPassword)
                        )
                    ).bind()
                }.toResponse(HttpStatusCode.OK).let { call.respond(it.code, it) }
            }
        }
    }
}