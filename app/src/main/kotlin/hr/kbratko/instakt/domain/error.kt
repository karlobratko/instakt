package hr.kbratko.instakt.domain

sealed interface DomainError

sealed interface MailingError : DomainError {
    data object CouldNotSendEmail : MailingError
}

data object UnhandledServerError : DomainError

data object EndpointRequestLimitMet : DomainError

sealed interface RequestError : DomainError {
    data object RequestBodyCouldNotBeParsed : RequestError

    data object InvalidRequestPathParameter : RequestError

    data object InvalidRequestQueryParameter : RequestError
}

sealed interface DbError : DomainError {

    data object UsernameAlreadyExists : DbError

    data object EmailAlreadyExists : DbError

    data object InvalidUsernameOrPassword : DbError

    data object InvalidPassword : DbError

    data object InvalidRefreshToken : DbError

    data object RefreshTokenAlreadyRevoked : DbError

    data object UnknownRegistrationToken : DbError

    data object RegistrationTokenAlreadyConfirmed : DbError

    data object RefreshTokenStillValid : DbError

    data object RegistrationTokenExpired : DbError

    data object RegistrationTokenStillValid : DbError

    data object RegistrationTokenNotConfirmed : DbError

    data object UserNotFound : DbError

    data object PasswordResetTokenStillValid : DbError

    data object UnknownPasswordResetToken : DbError

    data object SocialMediaLinkForPlatformAlreadyExists : DbError

    data object SocialMediaLinkNotFound : DbError
}

sealed interface SecurityError : DomainError {
    data object TokenGenerationFailed : SecurityError

    data object ClaimsExtractionError : SecurityError

    sealed interface TokenValidationError : SecurityError {
        data object MalformedSubject : SecurityError

        data object UnsupportedRoleClaim : SecurityError

        data object TokenExpired : SecurityError
    }
}

sealed interface ValidationError : DomainError {
    sealed interface UserValidationError : ValidationError {
        sealed interface UsernameValidationError : UserValidationError {
            data object NonAlphanumericCharacterInUsername : UsernameValidationError

            data object TooShortUsername : UsernameValidationError

            data object TooLongUsername : UsernameValidationError
        }

        sealed interface EmailValidationError : UserValidationError {
            data object InvalidEmail : EmailValidationError

            data object TooLongEmail : EmailValidationError
        }

        sealed interface FirstNameValidationError : UserValidationError {
            data object TooShortFirstName : FirstNameValidationError

            data object TooLongFirstName : FirstNameValidationError
        }

        sealed interface LastNameValidationError : UserValidationError {
            data object TooShortLastName : LastNameValidationError

            data object TooLongLastName : LastNameValidationError
        }

        sealed interface BioValidationError : UserValidationError {
            data object TooLongBio : BioValidationError
        }

        sealed interface PasswordValidationError : UserValidationError {
            data object TooShortPassword : PasswordValidationError

            data object NoDigitsInPassword : PasswordValidationError

            data object WhitespaceInPassword : PasswordValidationError

            data object NoUppercaseCharsInPassword : PasswordValidationError

            data object NoSpecialCharsInPassword : PasswordValidationError
        }

        sealed interface RoleValidationError : UserValidationError {
            data object InvalidUserRole : RoleValidationError

            data object AdminCanNotBeCreated : RoleValidationError
        }
    }

    sealed interface RedirectUrlValidationError : ValidationError {
        data object InvalidRedirectUrlPattern : RedirectUrlValidationError
    }

    sealed interface SocialMediaLinkValidationError : ValidationError {
        sealed interface UrlValidationError : SocialMediaLinkValidationError {
            data object TooLongUrl : UrlValidationError

            data object InvalidUrlPattern : UrlValidationError
        }

        sealed interface PlatformValidationError : SocialMediaLinkValidationError {
            data object TooShortPlatformName : PlatformValidationError

            data object TooLongPlatformName : PlatformValidationError
        }
    }
}
