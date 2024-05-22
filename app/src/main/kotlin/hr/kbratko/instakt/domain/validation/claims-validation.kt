package hr.kbratko.instakt.domain.validation

import arrow.core.raise.catch
import hr.kbratko.instakt.domain.SecurityError
import hr.kbratko.instakt.domain.SecurityError.TokenValidationError.MalformedSubject
import hr.kbratko.instakt.domain.SecurityError.TokenValidationError.TokenExpired
import hr.kbratko.instakt.domain.SecurityError.TokenValidationError.UnsupportedRoleClaim
import hr.kbratko.instakt.domain.applyWrapEitherNel
import hr.kbratko.instakt.domain.config.RoundedInstantProvider
import hr.kbratko.instakt.domain.model.User
import hr.kbratko.instakt.domain.security.jwt.Claims
import hr.kbratko.instakt.domain.toLongOrLeftNel

typealias SubjectValidationScope = ValidationScope<SecurityError, Claims.Subject>

val SubjectCanBeParsedToLong = SubjectValidationScope {
    value.toLongOrLeftNel { MalformedSubject }.map { this }
}

typealias RoleClaimValidationScope = ValidationScope<SecurityError, Claims.Role>

val RoleClaimCanBeParsedToRole = RoleClaimValidationScope {
    applyWrapEitherNel {
        catch({
            User.Role.valueOf(value)
        }) { raise(UnsupportedRoleClaim) }
    }
}

val TokenIsNotExpired = InstantAfterOrSameValidation(RoundedInstantProvider.now()) { TokenExpired }

typealias ClaimsValidationScope = ValidationScope<SecurityError, Claims>

val ClaimsAreValid = ClaimsValidationScope {
    validate(this) {
        with { validate(it.subject, SubjectCanBeParsedToLong) }
        with { validate(it.role, RoleClaimCanBeParsedToRole) }
        with { validate(it.expiresAt, TokenIsNotExpired) }
    }
}
