package hr.kbratko.instakt.infrastructure.routes.auth

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.toEitherNel
import hr.kbratko.instakt.domain.DbError.UnknownRegistrationToken
import hr.kbratko.instakt.domain.DbError.UserNotFound
import hr.kbratko.instakt.domain.DomainError
import hr.kbratko.instakt.domain.ValidationError.RedirectUrlValidationError.InvalidRedirectUrlPattern
import hr.kbratko.instakt.domain.utility.eitherNel
import hr.kbratko.instakt.domain.mailing.Email
import hr.kbratko.instakt.domain.mailing.MailingService
import hr.kbratko.instakt.domain.mailing.send
import hr.kbratko.instakt.domain.model.Plan
import hr.kbratko.instakt.domain.model.User
import hr.kbratko.instakt.domain.persistence.RegistrationTokenPersistence
import hr.kbratko.instakt.domain.persistence.UserPersistence
import hr.kbratko.instakt.domain.security.Token
import hr.kbratko.instakt.domain.validation.EmailIsValid
import hr.kbratko.instakt.domain.validation.FirstNameIsValid
import hr.kbratko.instakt.domain.validation.LastNameIsValid
import hr.kbratko.instakt.domain.validation.PasswordIsValid
import hr.kbratko.instakt.domain.validation.StringMatchingPatternValidation
import hr.kbratko.instakt.domain.validation.UsernameIsValid
import hr.kbratko.instakt.domain.validation.validate
import hr.kbratko.instakt.infrastructure.logging.ActionLogger
import hr.kbratko.instakt.infrastructure.mailing.Senders
import hr.kbratko.instakt.infrastructure.mailing.templates.ConfirmRegistration
import hr.kbratko.instakt.infrastructure.plugins.restrictedRateLimit
import hr.kbratko.instakt.infrastructure.routes.foldValidation
import hr.kbratko.instakt.infrastructure.routes.toResponse
import io.ktor.http.HttpStatusCode
import io.ktor.resources.Resource
import io.ktor.server.application.call
import io.ktor.server.plugins.requestvalidation.RequestValidationConfig
import io.ktor.server.plugins.requestvalidation.ValidationResult.Invalid
import io.ktor.server.plugins.requestvalidation.ValidationResult.Valid
import io.ktor.server.resources.post
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject

@Resource("/register")
data class Register(val parent: Auth = Auth()) {
    @Serializable
    data class Body(
        val username: String,
        val email: String,
        val firstName: String,
        val lastName: String,
        val password: String,
        val redirectUrl: String,
        val plan: Plan
    )

    @Resource("/confirm")
    data class Confirm(val parent: Register = Register()) {
        @Serializable data class Body(val token: String)
    }

    @Resource("/reset")
    data class Reset(val parent: Register = Register()) {
        @Serializable sealed interface Body {
            val redirectUrl: String

            @Serializable
            @SerialName("token")
            data class WithToken(val token: String, override val redirectUrl: String) : Body

            @Serializable
            @SerialName("email")
            data class WithEmail(val email: String, override val redirectUrl: String) : Body
        }

    }
}

fun RequestValidationConfig.registerValidation() {
    validate<Register.Body> { request ->
        validate(request) {
            with { it.username.validate(UsernameIsValid) }
            with { it.email.validate(EmailIsValid) }
            with { it.firstName.validate(FirstNameIsValid) }
            with { it.lastName.validate(LastNameIsValid) }
            with { it.password.validate(PasswordIsValid) }
            with { it.redirectUrl.validate(StringMatchingPatternValidation("^(https?)://(localhost|(([a-zA-Z0-9.-]+)\\.([a-zA-Z]{2,}))|((25[0-5]|2[0-4][0-9]|[01]?[0-9]?[0-9])\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9]?[0-9]))(:[0-9]+)?(/[a-zA-Z0-9.-]*)*/?$".toRegex()) { InvalidRedirectUrlPattern }) }
        }.foldValidation()
    }

    validate<Register.Reset.Body> { request ->
        when (request) {
            is Register.Reset.Body.WithEmail -> request.email.validate(EmailIsValid).fold(
                ifLeft = { errors -> Invalid(errors.map { it.toString() }) },
                ifRight = { Valid }
            )

            is Register.Reset.Body.WithToken -> Valid
        }
    }
}

fun Route.register() {
    val userPersistence by inject<UserPersistence>()
    val registrationTokenPersistence by inject<RegistrationTokenPersistence>()
    val senders by inject<Senders>()
    val mailingService by inject<MailingService>()
    val actionLogger by inject<ActionLogger>()

    val sendConfirmRegistrationEmail: suspend (User, Token.Register, String) -> Either<DomainError, Email> =
        { user, registrationToken, redirectUrl ->
            mailingService
                .send(
                    ConfirmRegistration(
                        from = senders.auth,
                        email = user.email,
                        username = user.username,
                        confirmUrl = "${redirectUrl}?registrationToken=${registrationToken.value}"
                    )
                )
        }

    restrictedRateLimit {
        post<Register, Register.Body> { _, body ->
            eitherNel<DomainError, Unit> {
                val user = userPersistence.insert(
                    User.New(
                        User.Username(body.username),
                        User.Email(body.email),
                        User.FirstName(body.firstName),
                        User.LastName(body.lastName),
                        User.Password(body.password),
                        body.plan
                    )
                ).toEitherNel().bind()

                val registrationToken = registrationTokenPersistence.insert(user.id).toEitherNel().bind()

                sendConfirmRegistrationEmail(user, registrationToken, body.redirectUrl).toEitherNel().bind()

                actionLogger.logUserRegistration(user.id)
            }.toResponse(HttpStatusCode.Created).let { call.respond(it.code, it) }
        }

        post<Register.Confirm, Register.Confirm.Body> { _, body ->
            either<DomainError, Unit> {
                registrationTokenPersistence.confirm(Token.Register(body.token)).bind()
            }.toResponse(HttpStatusCode.OK).let { call.respond(it.code, it) }
        }

        post<Register.Reset, Register.Reset.Body> { _, body ->
            either<DomainError, Unit> {
                val user = when (body) {
                    is Register.Reset.Body.WithEmail -> userPersistence.select(User.Email(body.email))
                        .toEither { UserNotFound }

                    is Register.Reset.Body.WithToken -> userPersistence.select(Token.Register(body.token))
                        .toEither { UnknownRegistrationToken }
                }.bind()

                val registrationToken = registrationTokenPersistence.insert(user.id).bind()
                sendConfirmRegistrationEmail(user, registrationToken, body.redirectUrl).bind()
            }.toResponse(HttpStatusCode.OK).let { call.respond(it.code, it) }
        }
    }
}
