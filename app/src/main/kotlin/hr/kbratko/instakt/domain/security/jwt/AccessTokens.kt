package hr.kbratko.instakt.domain.security.jwt

import arrow.core.Either
import hr.kbratko.instakt.domain.SecurityError
import hr.kbratko.instakt.domain.security.Token.Access

interface AccessTokens {
    suspend fun Claims.generate(secret: String): Either<SecurityError, Access>

    suspend fun Access.extract(secret: String): Either<SecurityError, Claims>
}
