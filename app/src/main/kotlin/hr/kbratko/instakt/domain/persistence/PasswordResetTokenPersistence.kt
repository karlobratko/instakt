package hr.kbratko.instakt.domain.persistence

import arrow.core.Either
import arrow.core.Option
import hr.kbratko.instakt.domain.DomainError
import hr.kbratko.instakt.domain.model.User
import hr.kbratko.instakt.domain.security.Token

interface PasswordResetTokenPersistence {
    suspend fun insert(userId: User.Id): Either<DomainError, Token.PasswordReset>

    suspend fun selectUserId(token: Token.PasswordReset): Option<User.Id>

    suspend fun delete(token: Token.PasswordReset): Either<DomainError, Token.PasswordReset>
}
