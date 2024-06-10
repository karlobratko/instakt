package hr.kbratko.instakt.domain.persistence

import arrow.core.Either
import arrow.core.Option
import hr.kbratko.instakt.domain.DomainError
import hr.kbratko.instakt.domain.model.User
import hr.kbratko.instakt.domain.model.User.Password
import hr.kbratko.instakt.domain.model.User.Username
import hr.kbratko.instakt.domain.security.Token

interface UserPersistence {
    suspend fun insert(user: User.New): Either<DomainError, User>

    suspend fun select(username: Username): Option<User>

    suspend fun select(email: User.Email): Option<User>

    suspend fun select(token: Token.Register): Option<User>

    suspend fun selectProfile(id: User.Id): Option<User.Profile>

    suspend fun select(username: Username, password: Password): Either<DomainError, User>

    suspend fun update(data: User.Edit): Either<DomainError, User.Profile>

    suspend fun update(data: User.ChangePassword): Either<DomainError, Unit>

    suspend fun update(data: User.ResetPassword): Either<DomainError, Unit>
}
