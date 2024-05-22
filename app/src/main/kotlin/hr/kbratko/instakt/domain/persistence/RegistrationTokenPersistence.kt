package hr.kbratko.instakt.domain.persistence

import arrow.core.Either
import arrow.core.Option
import hr.kbratko.instakt.domain.DomainError
import hr.kbratko.instakt.domain.model.RegistrationToken
import hr.kbratko.instakt.domain.security.Token

interface RegistrationTokenPersistence {
    suspend fun insert(): Token.Register

    suspend fun select(token: Token.Register): Option<RegistrationToken>

    suspend fun confirm(token: Token.Register): Either<DomainError, Token.Register>

    suspend fun reset(token: Token.Register): Either<DomainError, Token.Register>
}
