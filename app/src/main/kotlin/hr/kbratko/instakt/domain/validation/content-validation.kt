package hr.kbratko.instakt.domain.validation

import arrow.core.raise.ensure
import hr.kbratko.instakt.domain.ValidationError.ContentValidationError.ContentDescriptionValidationError
import hr.kbratko.instakt.domain.ValidationError.ContentValidationError.ContentDescriptionValidationError.TooLongContentDescription
import hr.kbratko.instakt.domain.ValidationError.ContentValidationError.ContentSizeValidationError
import hr.kbratko.instakt.domain.ValidationError.ContentValidationError.ContentSizeValidationError.MaxContentSizeExceeded
import hr.kbratko.instakt.domain.applyWrapEitherNel

typealias ContentDescriptionValidationScope = ValidationScope<ContentDescriptionValidationError, String>

val ContentDescriptionIsValid = ContentDescriptionValidationScope {
    validate(this) {
        with(StringMaxLengthValidation(1024) { TooLongContentDescription })
    }
}

typealias ContentTagsValidationScope = ValidationScope<ContentDescriptionValidationError, List<String>>

val ContentTagsAreValid = ContentTagsValidationScope {
    validate(this) {
        with(ListAllSatisfiesPredicateValidation({ it.length <= 50 }) { TooLongContentDescription })
    }
}

typealias ContentSizeValidationScope = ValidationScope<ContentSizeValidationError, ByteArray>

const val MAX_FILE_SIZE = 4 * 1024 * 1024

val ContentSizeIsValid = ContentSizeValidationScope {
    applyWrapEitherNel {
        ensure(size <= MAX_FILE_SIZE) { MaxContentSizeExceeded }
    }
}