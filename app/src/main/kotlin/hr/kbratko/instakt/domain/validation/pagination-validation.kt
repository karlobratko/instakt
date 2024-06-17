package hr.kbratko.instakt.domain.validation

import hr.kbratko.instakt.domain.ValidationError.PaginationValidationError
import hr.kbratko.instakt.domain.ValidationError.PaginationValidationError.CountValidation.TooBigPageCount
import hr.kbratko.instakt.domain.ValidationError.PaginationValidationError.CountValidation.NegativePageCount
import hr.kbratko.instakt.domain.ValidationError.PaginationValidationError.PageNumberValidation.NegativePageNumber
import hr.kbratko.instakt.domain.persistence.pagination.Page

typealias PaginationValidationScope = ValidationScope<PaginationValidationError, Page>

fun PaginationIsValid(maxCount: Int) = PaginationValidationScope {
    validate(this) {
        with { validate(it.count, IntGreaterThanOrEqualValidation(0) { NegativePageCount }) }
        with { validate(it.count, IntLessThanOrEqualValidation(maxCount) { TooBigPageCount }) }
        with { validate(it.number, LongGreaterThanOrEqualValidation(0) { NegativePageNumber }) }
    }
}
