package hr.kbratko.instakt.domain.persistence

import arrow.core.Either
import arrow.core.Option
import hr.kbratko.instakt.domain.DomainError
import hr.kbratko.instakt.domain.model.User
import hr.kbratko.instakt.domain.model.User.Password
import hr.kbratko.instakt.domain.model.User.Username

interface UserPersistence {
    suspend fun insert(user: User.New): Either<DomainError, User>

    suspend fun select(username: Username): Option<User>

    suspend fun select(id: User.Id): Option<User>

    suspend fun select(username: Username, password: Password): Either<DomainError, User>

    suspend fun update(data: User.Edit): Either<DomainError, User>

    suspend fun update(data: User.ChangePassword): Either<DomainError, User>

    suspend fun delete(id: User.Id): Either<DomainError, User.Id>
}
