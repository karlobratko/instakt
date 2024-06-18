package hr.kbratko.instakt.domain.validation

import arrow.core.raise.ensure
import hr.kbratko.instakt.domain.ValidationError.AuditLogValidationError.AuditLogSortTermError
import hr.kbratko.instakt.domain.ValidationError.AuditLogValidationError.AuditLogSortTermError.UnsupportedSortTerm
import hr.kbratko.instakt.domain.utility.applyWrapEitherNel

typealias AuditLogSortTermValidationScope = ValidationScope<AuditLogSortTermError, String>

private val VALID_SORT_TERMS = listOf("userId", "action", "affectedResource", "executedAt")

val AuditLogSortTermIsValid = AuditLogSortTermValidationScope validation@{
    applyWrapEitherNel {
        ensure(VALID_SORT_TERMS.contains(this@validation)) { UnsupportedSortTerm }
    }
}