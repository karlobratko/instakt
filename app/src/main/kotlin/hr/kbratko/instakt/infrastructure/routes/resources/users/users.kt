package hr.kbratko.instakt.infrastructure.routes.resources.users

import arrow.core.raise.either
import hr.kbratko.instakt.domain.DbError.UserNotFound
import hr.kbratko.instakt.domain.model.User
import hr.kbratko.instakt.domain.persistence.UserPersistence
import hr.kbratko.instakt.infrastructure.plugins.permissiveRateLimit
import hr.kbratko.instakt.infrastructure.routes.resources.Resources
import hr.kbratko.instakt.infrastructure.routes.toResponse
import io.ktor.http.HttpStatusCode
import io.ktor.resources.Resource
import io.ktor.server.application.call
import io.ktor.server.resources.get
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import org.koin.ktor.ext.inject

@Resource("/users/{id}")
data class Users(val parent: Resources = Resources(), val id: Long)

fun Route.users() {
    val userPersistence by inject<UserPersistence>()

    social()
    images()

    permissiveRateLimit {
        get<Users> { resource ->
            either {
                userPersistence.selectProfile(User.Id(resource.id)).toEither { UserNotFound }.bind()
            }.toResponse(HttpStatusCode.OK).let { call.respond(it.code, it) }
        }
    }
}