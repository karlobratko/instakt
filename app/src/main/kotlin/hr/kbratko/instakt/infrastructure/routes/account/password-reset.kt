package hr.kbratko.instakt.infrastructure.routes.account

import arrow.core.raise.either
import hr.kbratko.instakt.domain.DbError.UnknownPasswordResetToken
import hr.kbratko.instakt.domain.DbError.UserNotFound
import hr.kbratko.instakt.domain.DomainError
import hr.kbratko.instakt.domain.mailing.MailingService
import hr.kbratko.instakt.domain.mailing.send
import hr.kbratko.instakt.domain.model.User
import hr.kbratko.instakt.domain.persistence.PasswordResetTokenPersistence
import hr.kbratko.instakt.domain.persistence.UserPersistence
import hr.kbratko.instakt.domain.security.Token
import hr.kbratko.instakt.infrastructure.mailing.Senders
import hr.kbratko.instakt.infrastructure.mailing.templates.ResetPassword
import hr.kbratko.instakt.infrastructure.plugins.restrictedRateLimit
import hr.kbratko.instakt.infrastructure.routes.toResponse
import io.ktor.http.HttpStatusCode
import io.ktor.resources.Resource
import io.ktor.server.application.call
import io.ktor.server.resources.post
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject

@Resource("/password-reset")
data class PasswordReset(val parent: Account = Account()) {
    @Serializable data class Body(
        val token: String,
        val newPassword: String
    )

    @Resource("/acquire")
    data class Acquire(val parent: PasswordReset = PasswordReset()) {
        @Serializable data class Body(
            val redirectUrl: String,
            val email: String
        )
    }
}

fun Route.passwordReset() {
    val userPersistence by inject<UserPersistence>()
    val passwordResetTokenPersistence by inject<PasswordResetTokenPersistence>()
    val senders by inject<Senders>()
    val mailingService by inject<MailingService>()

    restrictedRateLimit {
        post<PasswordReset.Acquire, PasswordReset.Acquire.Body> { _, body ->
            either<DomainError, Unit> {
                val user = userPersistence.select(User.Email(body.email)).toEither { UserNotFound }.bind()
                val token = passwordResetTokenPersistence.insert(user.id).bind()

                mailingService
                    .send(
                        ResetPassword(
                            from = senders.auth,
                            email = user.email,
                            username = user.username,
                            resetUrl = "${body.redirectUrl}?resetPasswordToken=${token.value}"
                        )
                    )
                    .bind()
            }.toResponse(HttpStatusCode.Created).let { call.respond(it.code, it) }
        }

        post<PasswordReset, PasswordReset.Body> { _, body ->
            either<DomainError, Unit> {
                val token = Token.PasswordReset(body.token)
                val userId = passwordResetTokenPersistence.selectUserId(token)
                    .toEither { UnknownPasswordResetToken }.bind()

                userPersistence.update(
                    User.ResetPassword(
                        userId,
                        User.Password(body.newPassword)
                    )
                ).bind()

                passwordResetTokenPersistence.delete(token).bind()
            }.toResponse(HttpStatusCode.OK).let { call.respond(it.code, it) }
        }
    }
}