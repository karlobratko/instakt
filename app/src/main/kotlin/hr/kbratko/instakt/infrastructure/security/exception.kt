package hr.kbratko.instakt.infrastructure.security

import arrow.core.Nel
import hr.kbratko.instakt.domain.DomainError

class AuthenticationException(val errors: Nel<DomainError>) : Exception()