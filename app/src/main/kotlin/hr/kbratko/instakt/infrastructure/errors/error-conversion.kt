package hr.kbratko.instakt.infrastructure.errors

import arrow.core.Nel
import hr.kbratko.instakt.domain.DbError
import hr.kbratko.instakt.domain.DomainError
import hr.kbratko.instakt.domain.EndpointRequestLimitMet
import hr.kbratko.instakt.domain.MailingError
import hr.kbratko.instakt.domain.RequestError
import hr.kbratko.instakt.domain.SecurityError
import hr.kbratko.instakt.domain.UnhandledServerError
import hr.kbratko.instakt.domain.ValidationError
import hr.kbratko.instakt.domain.conversion.ConversionScope
import io.ktor.http.HttpStatusCode
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.Forbidden
import io.ktor.http.HttpStatusCode.Companion.InternalServerError
import io.ktor.http.HttpStatusCode.Companion.NotFound
import io.ktor.http.HttpStatusCode.Companion.TooManyRequests
import io.ktor.http.HttpStatusCode.Companion.Unauthorized

typealias NelErrorToHttpStatusCodeConversionScope = ConversionScope<Nel<DomainError>, HttpStatusCode>

val NelErrorToHttpStatusCodeConversion = NelErrorToHttpStatusCodeConversionScope {
    map {
        with(ErrorToHttpStatusCodeConversion) {
            it.convert()
        }
    }.reduce { acc, cur ->
        when (cur) {
            InternalServerError -> cur
            Unauthorized -> if (acc != InternalServerError) cur else acc
            Forbidden -> if (acc != InternalServerError && acc != Unauthorized) cur else acc
            else -> if (cur.value > acc.value) cur else acc
        }
    }
}

typealias ErrorToHttpStatusCodeConversionScope = ConversionScope<DomainError, HttpStatusCode>

val ErrorToHttpStatusCodeConversion = ErrorToHttpStatusCodeConversionScope {
    when (this) {
        DbError.EmailAlreadyExists -> BadRequest
        DbError.InvalidRefreshToken -> NotFound
        DbError.InvalidRegistrationToken -> NotFound
        DbError.InvalidUsernameOrPassword -> NotFound
        DbError.RefreshTokenAlreadyRevoked -> BadRequest
        DbError.RefreshTokenStillValid -> BadRequest
        DbError.RegistrationTokenAlreadyConfirmed -> BadRequest
        DbError.RegistrationTokenExpired -> BadRequest
        DbError.RegistrationTokenNotConfirmed -> BadRequest
        DbError.RegistrationTokenStillValid -> BadRequest
        DbError.UserNotFound -> NotFound
        DbError.UsernameAlreadyExists -> BadRequest
        MailingError.CouldNotSendEmail -> InternalServerError
        SecurityError.ClaimsExtractionError -> Unauthorized
        SecurityError.TokenValidationError.MalformedSubject -> Unauthorized
        SecurityError.TokenValidationError.TokenExpired -> Unauthorized
        SecurityError.TokenGenerationFailed -> InternalServerError
        SecurityError.TokenValidationError.UnsupportedRoleClaim -> Unauthorized
        ValidationError.UserValidationError.EmailValidationError.InvalidEmail -> BadRequest
        ValidationError.UserValidationError.EmailValidationError.TooLongEmail -> BadRequest
        ValidationError.UserValidationError.PasswordValidationError.NoDigitsInPassword -> BadRequest
        ValidationError.UserValidationError.PasswordValidationError.NoSpecialCharsInPassword -> BadRequest
        ValidationError.UserValidationError.PasswordValidationError.NoUppercaseCharsInPassword -> BadRequest
        ValidationError.UserValidationError.PasswordValidationError.TooShortPassword -> BadRequest
        ValidationError.UserValidationError.PasswordValidationError.WhitespaceInPassword -> BadRequest
        ValidationError.UserValidationError.UsernameValidationError.NonAlphanumericCharacterInUsername -> BadRequest
        ValidationError.UserValidationError.UsernameValidationError.TooLongUsername -> BadRequest
        ValidationError.UserValidationError.UsernameValidationError.TooShortUsername -> BadRequest
        RequestError.InvalidRequestPathParameter -> BadRequest
        RequestError.InvalidRequestQueryParameter -> BadRequest
        RequestError.RequestBodyCouldNotBeParsed -> BadRequest
        UnhandledServerError -> InternalServerError
        EndpointRequestLimitMet -> TooManyRequests
        ValidationError.UserValidationError.RoleValidationError.AdminCanNotBeCreated -> BadRequest
        ValidationError.UserValidationError.RedirectUrlValidationError.InvalidRedirectUrlPattern -> BadRequest
        ValidationError.UserValidationError.RoleValidationError.InvalidUserRole -> BadRequest
    }
}
