package hr.kbratko.instakt.domain.validation

import arrow.core.raise.ensure
import hr.kbratko.instakt.domain.ValidationError.UserValidationError.EmailValidationError
import hr.kbratko.instakt.domain.ValidationError.UserValidationError.EmailValidationError.InvalidEmail
import hr.kbratko.instakt.domain.ValidationError.UserValidationError.EmailValidationError.TooLongEmail
import hr.kbratko.instakt.domain.ValidationError.UserValidationError.PasswordValidationError
import hr.kbratko.instakt.domain.ValidationError.UserValidationError.PasswordValidationError.NoDigitsInPassword
import hr.kbratko.instakt.domain.ValidationError.UserValidationError.PasswordValidationError.NoSpecialCharsInPassword
import hr.kbratko.instakt.domain.ValidationError.UserValidationError.PasswordValidationError.NoUppercaseCharsInPassword
import hr.kbratko.instakt.domain.ValidationError.UserValidationError.PasswordValidationError.TooShortPassword
import hr.kbratko.instakt.domain.ValidationError.UserValidationError.PasswordValidationError.WhitespaceInPassword
import hr.kbratko.instakt.domain.ValidationError.UserValidationError.RoleValidationError
import hr.kbratko.instakt.domain.ValidationError.UserValidationError.UsernameValidationError
import hr.kbratko.instakt.domain.ValidationError.UserValidationError.UsernameValidationError.NonAlphanumericCharacterInUsername
import hr.kbratko.instakt.domain.ValidationError.UserValidationError.UsernameValidationError.TooLongUsername
import hr.kbratko.instakt.domain.ValidationError.UserValidationError.UsernameValidationError.TooShortUsername
import hr.kbratko.instakt.domain.applyWrapEitherNel
import hr.kbratko.instakt.domain.model.User

typealias UsernameValidationScope = ValidationScope<UsernameValidationError, String>

val UsernameIsValid = UsernameValidationScope {
    validate(this) {
        with(StringAllSatisfiesPredicateValidation({ it.isLetterOrDigit() }) { NonAlphanumericCharacterInUsername })
        with(StringMinLengthValidation(5) { TooShortUsername })
        with(StringMaxLengthValidation(50) { TooLongUsername })
    }
}

typealias EmailValidationScope = ValidationScope<EmailValidationError, String>

const val EMAIL_PATTERN = "^[A-Za-z0-9+!#\$%&'*+-/=?^_`{|}~.]+@[A-Za-z0-9.-]+\$"

val EmailIsValid = EmailValidationScope {
    validate(this) {
        with(StringMatchingPatternValidation(EMAIL_PATTERN.toRegex()) { InvalidEmail })
        with(StringMaxLengthValidation(256) { TooLongEmail })
    }
}

typealias PasswordValidationScope = ValidationScope<PasswordValidationError, String>

const val SPECIAL_CHARACTERS = "!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~"

private fun Char.isSpecialChar() = SPECIAL_CHARACTERS.contains(this)

val PasswordIsValid =
    PasswordValidationScope {
        validate(this) {
            with(StringMinLengthValidation(5) { TooShortPassword })
            with(StringNoneSatisfiesPredicateValidation({ it.isWhitespace() }) { WhitespaceInPassword })
            with(StringAnySatisfiesPredicateValidation({ it.isDigit() }) { NoDigitsInPassword })
            with(StringAnySatisfiesPredicateValidation({ it.isUpperCase() }) { NoUppercaseCharsInPassword })
            with(StringAnySatisfiesPredicateValidation({ it.isSpecialChar() }) { NoSpecialCharsInPassword })
        }
    }

typealias RoleValidationScope = ValidationScope<RoleValidationError, User.Role>

val ALLOWED_CREATION_ROLES = listOf(User.Role.User)

val RoleIsNotAdmin = RoleValidationScope {
    applyWrapEitherNel {
        ensure(this@RoleValidationScope in ALLOWED_CREATION_ROLES) { RoleValidationError.AdminCanNotBeCreated }
    }
}
