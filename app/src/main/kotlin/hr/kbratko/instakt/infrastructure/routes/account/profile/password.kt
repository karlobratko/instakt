package hr.kbratko.instakt.infrastructure.routes.account.profile

import arrow.core.raise.either
import hr.kbratko.instakt.domain.DomainError
import hr.kbratko.instakt.domain.model.User
import hr.kbratko.instakt.domain.persistence.UserPersistence
import hr.kbratko.instakt.domain.validation.PasswordIsValid
import hr.kbratko.instakt.domain.validation.validate
import hr.kbratko.instakt.infrastructure.ktor.principal
import hr.kbratko.instakt.infrastructure.logging.ActionLogger
import hr.kbratko.instakt.infrastructure.plugins.jwt
import hr.kbratko.instakt.infrastructure.plugins.restrictedRateLimit
import hr.kbratko.instakt.infrastructure.routes.foldValidation
import hr.kbratko.instakt.infrastructure.routes.toResponse
import hr.kbratko.instakt.infrastructure.security.UserPrincipal
import io.ktor.http.HttpStatusCode
import io.ktor.resources.Resource
import io.ktor.server.application.call
import io.ktor.server.plugins.requestvalidation.RequestValidationConfig
import io.ktor.server.resources.put
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject

@Resource("/password")
data class Password(val parent: UserProfile = UserProfile()) {
    @Serializable data class Body(
        val oldPassword: String,
        val newPassword: String
    )
}

fun RequestValidationConfig.passwordValidation() {
    validate<Password.Body> { request ->
        validate(request) {
            with { it.oldPassword.validate(PasswordIsValid) }
            with { it.newPassword.validate(PasswordIsValid) }
        }.foldValidation()
    }
}

fun Route.password() {
    val userPersistence by inject<UserPersistence>()
    val actionLogger by inject<ActionLogger>()

    restrictedRateLimit {
        jwt {
            put<Password, Password.Body> { _, body ->
                either<DomainError, Unit> {
                    val principal = call.principal<UserPrincipal>()

                    userPersistence.update(
                        User.ChangePassword(
                            principal.id,
                            User.Password(body.oldPassword),
                            User.Password(body.newPassword)
                        )
                    ).bind()

                    actionLogger.logPasswordReset(principal.id)
                }.toResponse(HttpStatusCode.OK).let { call.respond(it.code, it) }
            }
        }
    }
}