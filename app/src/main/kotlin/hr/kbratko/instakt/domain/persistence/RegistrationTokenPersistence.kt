package hr.kbratko.instakt.domain.persistence

import arrow.core.Either
import hr.kbratko.instakt.domain.DomainError
import hr.kbratko.instakt.domain.model.User
import hr.kbratko.instakt.domain.security.Token

interface RegistrationTokenPersistence {
    suspend fun insert(userId: User.Id): Either<DomainError, Token.Register>

    suspend fun confirm(token: Token.Register): Either<DomainError, Token.Register>
}
