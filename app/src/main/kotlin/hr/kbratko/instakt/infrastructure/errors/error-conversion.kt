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
        // BadRequest
        DbError.EmailAlreadyExists,
        DbError.RefreshTokenAlreadyRevoked,
        DbError.RefreshTokenStillValid,
        DbError.RegistrationTokenAlreadyConfirmed,
        DbError.RegistrationTokenExpired,
        DbError.RegistrationTokenNotConfirmed,
        DbError.RegistrationTokenStillValid,
        DbError.UsernameAlreadyExists,
        ValidationError.UserValidationError.EmailValidationError.InvalidEmail,
        ValidationError.UserValidationError.EmailValidationError.TooLongEmail,
        ValidationError.UserValidationError.PasswordValidationError.NoDigitsInPassword,
        ValidationError.UserValidationError.PasswordValidationError.NoSpecialCharsInPassword,
        ValidationError.UserValidationError.PasswordValidationError.NoUppercaseCharsInPassword,
        ValidationError.UserValidationError.PasswordValidationError.TooShortPassword,
        ValidationError.UserValidationError.PasswordValidationError.WhitespaceInPassword,
        ValidationError.UserValidationError.UsernameValidationError.NonAlphanumericCharacterInUsername,
        ValidationError.UserValidationError.UsernameValidationError.TooLongUsername,
        ValidationError.UserValidationError.UsernameValidationError.TooShortUsername,
        RequestError.RequestCouldNotBeProcessed,
        ValidationError.UserValidationError.RoleValidationError.AdminCanNotBeCreated,
        ValidationError.UserValidationError.RoleValidationError.InvalidUserRole,
        ValidationError.UserValidationError.FirstNameValidationError.TooLongFirstName,
        ValidationError.UserValidationError.FirstNameValidationError.TooShortFirstName,
        ValidationError.UserValidationError.LastNameValidationError.TooLongLastName,
        ValidationError.UserValidationError.LastNameValidationError.TooShortLastName,
        ValidationError.UserValidationError.BioValidationError.TooLongBio,
        DbError.InvalidPassword,
        DbError.PasswordResetTokenStillValid,
        DbError.SocialMediaLinkForPlatformAlreadyExists,
        ValidationError.RedirectUrlValidationError.InvalidRedirectUrlPattern,
        ValidationError.SocialMediaLinkValidationError.PlatformValidationError.TooLongPlatformName,
        ValidationError.SocialMediaLinkValidationError.PlatformValidationError.TooShortPlatformName,
        ValidationError.SocialMediaLinkValidationError.UrlValidationError.InvalidUrlPattern,
        ValidationError.SocialMediaLinkValidationError.UrlValidationError.TooLongUrl,
        DbError.CouldNotDeleteContent,
        DbError.CouldNotPersistContent,
        DbError.UnsupportedContentType,
        ValidationError.ContentValidationError.ContentDescriptionValidationError.TooLongContentDescription,
        ValidationError.ContentValidationError.ContentTagsValidationError.TooLongContentTagName,
        ValidationError.ContentValidationError.ContentTagsValidationError.TooShortContentTagName,
        ValidationError.ContentValidationError.ContentSizeValidationError.MaxContentSizeExceeded,
        ValidationError.ContentValidationError.ContentSortTermError.UnsupportedSortTerm,
        ValidationError.ContentValidationError.ContentUploadRangeError.StartDateIsAfterEndDate,
        ValidationError.PaginationValidationError.CountValidation.TooBigPageCount,
        ValidationError.PaginationValidationError.CountValidation.NegativePageCount,
        ValidationError.PaginationValidationError.PageNumberValidation.NegativePageNumber -> BadRequest

        // NotFound
        DbError.InvalidRefreshToken,
        DbError.UnknownRegistrationToken,
        DbError.InvalidUsernameOrPassword,
        DbError.UserNotFound,
        DbError.UnknownPasswordResetToken,
        DbError.SocialMediaLinkNotFound,
        DbError.ContentMetadataNotFound,
        DbError.ContentNotFound,
        DbError.ProfilePictureMetadataNotFound -> NotFound

        // Unauthorized
        SecurityError.ClaimsExtractionError,
        SecurityError.TokenValidationError.MalformedSubject,
        SecurityError.TokenValidationError.TokenExpired,
        SecurityError.TokenValidationError.UnsupportedRoleClaim -> Unauthorized

        // InternalServerError
        MailingError.CouldNotSendEmail,
        SecurityError.TokenGenerationFailed,
        UnhandledServerError -> InternalServerError

        // TooManyRequests
        EndpointRequestLimitMet -> TooManyRequests
    }
}
