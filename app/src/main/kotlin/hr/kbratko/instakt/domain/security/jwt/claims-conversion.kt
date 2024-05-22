package hr.kbratko.instakt.domain.security.jwt

import hr.kbratko.instakt.domain.conversion.ConversionScope
import hr.kbratko.instakt.domain.model.User

typealias UserIdToSubjectConversionScope = ConversionScope<User.Id, Claims.Subject>

val UserIdToSubjectConversion = UserIdToSubjectConversionScope { Claims.Subject(value.toString()) }

typealias SubjectToUserIdConversionScope = ConversionScope<Claims.Subject, User.Id>

val SubjectToUserIdConversion = SubjectToUserIdConversionScope {
    User.Id(value.toLong())
}

typealias RoleToRoleClaimConversionScope = ConversionScope<User.Role, Claims.Role>

val RoleToRoleClaimConversion = RoleToRoleClaimConversionScope { Claims.Role(name) }

typealias RoleClaimToRoleConversionScope = ConversionScope<Claims.Role, User.Role>

val RoleClaimToRoleConversion = RoleClaimToRoleConversionScope {
    User.Role.valueOf(value)
}
