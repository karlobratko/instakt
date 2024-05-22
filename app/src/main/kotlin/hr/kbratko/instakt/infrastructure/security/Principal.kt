package hr.kbratko.instakt.infrastructure.security

import hr.kbratko.instakt.domain.model.User
import io.ktor.server.auth.Principal

data class UserPrincipal(val id: User.Id, val role: User.Role) : Principal
