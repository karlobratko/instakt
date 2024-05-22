package hr.kbratko.instakt.infrastructure.routes.auth

import arrow.core.toEitherNel
import hr.kbratko.instakt.domain.DomainError
import hr.kbratko.instakt.domain.ValidationError
import hr.kbratko.instakt.domain.ValidationError.UserValidationError.RedirectUrlValidationError.InvalidRedirectUrlPattern
import hr.kbratko.instakt.domain.ValidationError.UserValidationError.RoleValidationError.InvalidUserRole
import hr.kbratko.instakt.domain.eitherNel
import hr.kbratko.instakt.domain.mailing.MailingService
import hr.kbratko.instakt.domain.mailing.send
import hr.kbratko.instakt.domain.model.User
import hr.kbratko.instakt.domain.persistence.RegistrationTokenPersistence
import hr.kbratko.instakt.domain.persistence.UserPersistence
import hr.kbratko.instakt.domain.security.Token
import hr.kbratko.instakt.domain.validation.EmailIsValid
import hr.kbratko.instakt.domain.validation.PasswordIsValid
import hr.kbratko.instakt.domain.validation.RoleIsNotAdmin
import hr.kbratko.instakt.domain.validation.StringIsEnumValidation
import hr.kbratko.instakt.domain.validation.StringMatchingPatternValidation
import hr.kbratko.instakt.domain.validation.UsernameIsValid
import hr.kbratko.instakt.domain.validation.validate
import hr.kbratko.instakt.infrastructure.mailing.Senders
import hr.kbratko.instakt.infrastructure.mailing.templates.ConfirmRegistration
import hr.kbratko.instakt.infrastructure.plugins.restrictedRateLimit
import hr.kbratko.instakt.infrastructure.routes.toResponse
import io.ktor.http.HttpStatusCode
import io.ktor.resources.Resource
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.plugins.requestvalidation.RequestValidation
import io.ktor.server.plugins.requestvalidation.ValidationResult.Invalid
import io.ktor.server.plugins.requestvalidation.ValidationResult.Valid
import io.ktor.server.request.receive
import io.ktor.server.routing.Route
import io.ktor.server.resources.post
import io.ktor.server.response.respond
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject

@Resource("/register")
data class Register(val parent: Auth = Auth()) {
    @Resource("/confirm")
    data class Confirm(val parent: Register = Register())

    @Resource("/reset")
    data class Reset(val parent: Register = Register())
}

fun Route.register() {
    val userPersistence by inject<UserPersistence>()
    val registrationTokenPersistence by inject<RegistrationTokenPersistence>()
    val senders by inject<Senders>()
    val mailingService by inject<MailingService>()

    install(RequestValidation) {
        val urlRegex =
            "^(https?)://(localhost|(([a-zA-Z0-9.-]+)\\.([a-zA-Z]{2,}))|((25[0-5]|2[0-4][0-9]|[01]?[0-9]?[0-9])\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9]?[0-9]))(:[0-9]+)?(/[a-zA-Z0-9.-]*)*/?$".toRegex()

        validate<RegisterUser> { request ->
            validate(request) {
                with { it.username.validate(UsernameIsValid) }
                with { it.email.validate(EmailIsValid) }
                with { it.password.validate(PasswordIsValid) }
                with { it.role.validate(StringIsEnumValidation<InvalidUserRole, User.Role> { InvalidUserRole }) }
                with { it.redirectUrl.validate(StringMatchingPatternValidation(urlRegex) { InvalidRedirectUrlPattern }) }
            }.fold(
                ifLeft = { errors -> Invalid(errors.map { it.toString() }) },
                ifRight = { Valid }
            )
        }
    }

    restrictedRateLimit {
        post<Register> {
            eitherNel<DomainError, Unit> {
                val content = call.receive<RegisterUser>()

                val user = userPersistence.insert(
                    User.New(
                        User.Username(content.username),
                        User.Email(content.email),
                        User.Password(content.password),
                        User.Role.valueOf(content.role).validate(RoleIsNotAdmin).bind()
                    )
                ).toEitherNel().bind()

                mailingService
                    .send(
                        ConfirmRegistration(
                            from = senders.auth,
                            email = user.email,
                            username = user.username,
                            confirmUrl = "${content.redirectUrl}?registrationToken=${user.registrationToken.value}"
                        )
                    )
                    .toEitherNel()
                    .onLeft { userPersistence.delete(user.id) }
                    .bind()
            }.toResponse(HttpStatusCode.Created).let { call.respond(it.code, it) }
        }

        post<Register.Confirm> {
            eitherNel<DomainError, Unit> {
                val content = call.receive<ConfirmRegister>()

                registrationTokenPersistence.confirm(Token.Register(content.registrationToken)).toEitherNel().bind()
            }.toResponse(HttpStatusCode.OK).let { call.respond(it.code, it) }
        }

        post<Register.Reset> {
            eitherNel<DomainError, Unit> {
                val content = call.receive<ResetRegister>()

                registrationTokenPersistence.reset(Token.Register(content.registrationToken)).toEitherNel().bind()
            }.toResponse(HttpStatusCode.OK).let { call.respond(it.code, it) }
        }
    }

}

@Serializable
data class RegisterUser(
    val username: String,
    val email: String,
    val password: String,
    val role: String,
    val redirectUrl: String
)

@Serializable data class ConfirmRegister(val registrationToken: String)

@Serializable data class ResetRegister(val registrationToken: String)
