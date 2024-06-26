package hr.kbratko.instakt.domain.validation

import arrow.core.raise.ensure
import hr.kbratko.instakt.domain.ValidationError.ContentValidationError.ContentDescriptionValidationError
import hr.kbratko.instakt.domain.ValidationError.ContentValidationError.ContentDescriptionValidationError.TooLongContentDescription
import hr.kbratko.instakt.domain.ValidationError.ContentValidationError.ContentSizeValidationError
import hr.kbratko.instakt.domain.ValidationError.ContentValidationError.ContentSizeValidationError.MaxContentSizeExceeded
import hr.kbratko.instakt.domain.ValidationError.ContentValidationError.ContentSortTermError
import hr.kbratko.instakt.domain.ValidationError.ContentValidationError.ContentSortTermError.UnsupportedSortTerm
import hr.kbratko.instakt.domain.utility.applyWrapEitherNel

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

const val MAX_FILE_SIZE = 4 * 1024 * 1024 // 4MB

val ContentSizeIsValid = ContentSizeValidationScope {
    applyWrapEitherNel {
        ensure(size <= MAX_FILE_SIZE) { MaxContentSizeExceeded }
    }
}

typealias ContentSortTermValidationScope = ValidationScope<ContentSortTermError, String>

private val VALID_SORT_TERMS = listOf("uploadedAt")

val ContentSortTermIsValid = ContentSortTermValidationScope validation@{
    applyWrapEitherNel {
        ensure(VALID_SORT_TERMS.contains(this@validation)) { UnsupportedSortTerm }
    }
}