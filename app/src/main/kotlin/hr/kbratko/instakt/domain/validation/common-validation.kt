package hr.kbratko.instakt.domain.validation

import arrow.core.raise.ensure
import hr.kbratko.instakt.domain.ValidationError.CommonValidationError.InstantRangeError
import hr.kbratko.instakt.domain.ValidationError.CommonValidationError.InstantRangeError.StartDateIsAfterEndDate
import hr.kbratko.instakt.domain.utility.InstantClosedRange
import hr.kbratko.instakt.domain.utility.applyWrapEitherNel

typealias ContentUploadRangeValidationScope = ValidationScope<InstantRangeError, InstantClosedRange>

val InstantRangeIsValid = ContentUploadRangeValidationScope validation@{
    applyWrapEitherNel {
        ensure(this@validation.start <= this@validation.endInclusive) { StartDateIsAfterEndDate }
    }
}
