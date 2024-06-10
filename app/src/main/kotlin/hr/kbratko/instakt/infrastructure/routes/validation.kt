package hr.kbratko.instakt.infrastructure.routes

import arrow.core.EitherNel
import hr.kbratko.instakt.domain.ValidationError
import hr.kbratko.instakt.domain.ValidationError.UserValidationError.RedirectUrlValidationError.InvalidRedirectUrlPattern
import hr.kbratko.instakt.domain.ValidationError.UserValidationError.RoleValidationError.InvalidUserRole
import hr.kbratko.instakt.domain.model.User
import hr.kbratko.instakt.domain.validation.BioIsValid
import hr.kbratko.instakt.domain.validation.EmailIsValid
import hr.kbratko.instakt.domain.validation.FirstNameIsValid
import hr.kbratko.instakt.domain.validation.LastNameIsValid
import hr.kbratko.instakt.domain.validation.PasswordIsValid
import hr.kbratko.instakt.domain.validation.StringIsEnumValidation
import hr.kbratko.instakt.domain.validation.StringMatchingPatternValidation
import hr.kbratko.instakt.domain.validation.UsernameIsValid
import hr.kbratko.instakt.domain.validation.validate
import hr.kbratko.instakt.infrastructure.routes.account.Profile
import hr.kbratko.instakt.infrastructure.routes.auth.Access
import hr.kbratko.instakt.infrastructure.routes.auth.Register
import io.ktor.server.application.install
import io.ktor.server.plugins.requestvalidation.RequestValidation
import io.ktor.server.plugins.requestvalidation.ValidationResult.Invalid
import io.ktor.server.plugins.requestvalidation.ValidationResult.Valid
import io.ktor.server.routing.Routing

private const val URL_PATTERN =
    "^(https?)://(localhost|(([a-zA-Z0-9.-]+)\\.([a-zA-Z]{2,}))|((25[0-5]|2[0-4][0-9]|[01]?[0-9]?[0-9])\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9]?[0-9]))(:[0-9]+)?(/[a-zA-Z0-9.-]*)*/?$"

private fun <T> EitherNel<ValidationError, T>.foldValidation() = fold(
    ifLeft = { errors -> Invalid(errors.map { it.toString() }) },
    ifRight = { Valid }
)

fun Routing.validation() {
    install(RequestValidation) {
        validate<Register.Body> { request ->
            validate(request) {
                with { it.username.validate(UsernameIsValid) }
                with { it.email.validate(EmailIsValid) }
                with { it.firstName.validate(FirstNameIsValid) }
                with { it.lastName.validate(LastNameIsValid) }
                with { it.password.validate(PasswordIsValid) }
                with { it.role.validate(StringIsEnumValidation<InvalidUserRole, User.Role> { InvalidUserRole }) }
                with { it.redirectUrl.validate(StringMatchingPatternValidation(URL_PATTERN.toRegex()) { InvalidRedirectUrlPattern }) }
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

        validate<Access.Acquire.Body> { request ->
            validate(request) {
                with { it.username.validate(UsernameIsValid) }
                with { it.password.validate(PasswordIsValid) }
            }.foldValidation()
        }

        validate<Profile.Body> { request ->
            validate(request) {
                with { it.firstName.validate(FirstNameIsValid) }
                with { it.lastName.validate(LastNameIsValid) }
                with { it.bio.validate(BioIsValid) }
            }.foldValidation()
        }

        validate<Profile.Password.Body> { request ->
            validate(request) {
                with { it.oldPassword.validate(PasswordIsValid) }
                with { it.newPassword.validate(PasswordIsValid) }
            }.foldValidation()
        }
    }
}