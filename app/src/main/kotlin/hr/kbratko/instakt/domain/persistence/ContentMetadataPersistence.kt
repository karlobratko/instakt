package hr.kbratko.instakt.domain.persistence

import arrow.core.Either
import arrow.core.Option
import arrow.core.none
import hr.kbratko.instakt.domain.DomainError
import hr.kbratko.instakt.domain.model.Content
import hr.kbratko.instakt.domain.model.ContentMetadata
import hr.kbratko.instakt.domain.model.User
import hr.kbratko.instakt.domain.persistence.pagination.Page
import hr.kbratko.instakt.domain.persistence.pagination.Sort

interface ContentMetadataPersistence {
    suspend fun insert(metadata: ContentMetadata.New): ContentMetadata

    suspend fun insert(metadata: ContentMetadata.NewProfile): Either<DomainError, ContentMetadata>

    suspend fun select(filter: ContentMetadata.Filter, page: Page, sort: Option<Sort> = none()): Set<ContentMetadata>

    suspend fun selectProfile(userId: User.Id): Option<ContentMetadata>

    suspend fun selectNonProfile(userId: User.Id): Set<ContentMetadata>

    suspend fun sumTotalUploadedBytes(userId: User.Id): Long

    suspend fun update(data: ContentMetadata.Edit): Either<DomainError, ContentMetadata>

    suspend fun delete(data: ContentMetadata.Delete): Either<DomainError, Content.Id>

    suspend fun delete(data: ContentMetadata.DeleteProfile): Either<DomainError, Content.Id>
}