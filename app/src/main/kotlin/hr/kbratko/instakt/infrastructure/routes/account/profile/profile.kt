package hr.kbratko.instakt.infrastructure.routes.account.profile

import arrow.core.raise.either
import hr.kbratko.instakt.domain.DbError.UserNotFound
import hr.kbratko.instakt.domain.model.User
import hr.kbratko.instakt.domain.persistence.UserPersistence
import hr.kbratko.instakt.domain.validation.BioIsValid
import hr.kbratko.instakt.domain.validation.FirstNameIsValid
import hr.kbratko.instakt.domain.validation.LastNameIsValid
import hr.kbratko.instakt.domain.validation.validate
import hr.kbratko.instakt.infrastructure.ktor.principal
import hr.kbratko.instakt.infrastructure.plugins.jwt
import hr.kbratko.instakt.infrastructure.plugins.permissiveRateLimit
import hr.kbratko.instakt.infrastructure.plugins.restrictedRateLimit
import hr.kbratko.instakt.infrastructure.routes.account.Account
import hr.kbratko.instakt.infrastructure.routes.account.profile.images.images
import hr.kbratko.instakt.infrastructure.routes.foldValidation
import hr.kbratko.instakt.infrastructure.routes.toResponse
import hr.kbratko.instakt.infrastructure.security.UserPrincipal
import io.ktor.http.HttpStatusCode
import io.ktor.resources.Resource
import io.ktor.server.application.call
import io.ktor.server.plugins.requestvalidation.RequestValidationConfig
import io.ktor.server.resources.get
import io.ktor.server.resources.put
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject

@Resource("/user")
data class UserProfile(val parent: Account = Account()) {
    @Serializable data class Body(
        val firstName: String,
        val lastName: String,
        val bio: String
    )
}

fun RequestValidationConfig.userProfileValidation() {
    validate<UserProfile.Body> { request ->
        validate(request) {
            with { it.firstName.validate(FirstNameIsValid) }
            with { it.lastName.validate(LastNameIsValid) }
            with { it.bio.validate(BioIsValid) }
        }.foldValidation()
    }
}

fun Route.profile() {
    val userPersistence by inject<UserPersistence>()

    social()
    password()
    images()

    permissiveRateLimit {
        jwt {
            get<UserProfile> {
                either {
                    val principal = call.principal<UserPrincipal>()
                    userPersistence.selectProfile(principal.id).toEither { UserNotFound }.bind()
                }.toResponse(HttpStatusCode.OK).let { call.respond(it.code, it) }
            }
        }
    }

    restrictedRateLimit {
        jwt {
            put<UserProfile, UserProfile.Body> { _, body ->
                either {
                    val principal = call.principal<UserPrincipal>()

                    userPersistence.update(
                        User.Edit(
                            principal.id,
                            User.FirstName(body.firstName),
                            User.LastName(body.lastName),
                            User.Bio(body.bio)
                        )
                    ).bind()
                }.toResponse(HttpStatusCode.OK).let { call.respond(it.code, it) }
            }
        }
    }
}