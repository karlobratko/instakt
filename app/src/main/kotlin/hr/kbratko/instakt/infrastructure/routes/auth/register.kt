package hr.kbratko.instakt.infrastructure.routes.auth

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.toEitherNel
import hr.kbratko.instakt.domain.DomainError
import hr.kbratko.instakt.domain.eitherNel
import hr.kbratko.instakt.domain.mailing.Email
import hr.kbratko.instakt.domain.mailing.MailingService
import hr.kbratko.instakt.domain.mailing.send
import hr.kbratko.instakt.domain.model.User
import hr.kbratko.instakt.domain.persistence.RegistrationTokenPersistence
import hr.kbratko.instakt.domain.persistence.UserPersistence
import hr.kbratko.instakt.domain.security.Token
import hr.kbratko.instakt.domain.validation.RoleIsNotAdmin
import hr.kbratko.instakt.domain.validation.validate
import hr.kbratko.instakt.infrastructure.mailing.Senders
import hr.kbratko.instakt.infrastructure.mailing.templates.ConfirmRegistration
import hr.kbratko.instakt.infrastructure.plugins.restrictedRateLimit
import hr.kbratko.instakt.infrastructure.routes.toResponse
import io.ktor.http.HttpStatusCode
import io.ktor.resources.Resource
import io.ktor.server.application.call
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
        val role: String,
        val redirectUrl: String
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

fun Route.register() {
    val userPersistence by inject<UserPersistence>()
    val registrationTokenPersistence by inject<RegistrationTokenPersistence>()
    val senders by inject<Senders>()
    val mailingService by inject<MailingService>()

    val sendConfirmRegistrationEmail: suspend (User, String) -> Either<DomainError, Email> = { user, redirectUrl ->
        mailingService
            .send(
                ConfirmRegistration(
                    from = senders.auth,
                    email = user.email,
                    username = user.username,
                    confirmUrl = "${redirectUrl}?registrationToken=${user.registrationToken.value}"
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
                        User.Role.valueOf(body.role).validate(RoleIsNotAdmin).bind()
                    )
                ).toEitherNel().bind()

                sendConfirmRegistrationEmail(user, body.redirectUrl).toEitherNel().bind()
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
                    is Register.Reset.Body.WithEmail -> userPersistence.resetRegistrationToken(User.Email(body.email))
                    is Register.Reset.Body.WithToken -> userPersistence.resetRegistrationToken(Token.Register(body.token))
                }.bind()

                sendConfirmRegistrationEmail(user, body.redirectUrl).bind()
            }.toResponse(HttpStatusCode.OK).let { call.respond(it.code, it) }
        }
    }
}
