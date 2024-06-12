package hr.kbratko.instakt.domain.persistence

import arrow.core.Either
import arrow.core.Option
import hr.kbratko.instakt.domain.DomainError
import hr.kbratko.instakt.domain.model.Content

interface ContentPersistence {
    suspend fun upload(content: Content.New): Either<DomainError, Content>

    suspend fun download(id: Content.Id): Option<Content>

    suspend fun remove(id: Content.Id): Either<DomainError, Content.Id>
}