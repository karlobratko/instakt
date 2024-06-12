package hr.kbratko.instakt.domain.persistence

import arrow.core.Either
import hr.kbratko.instakt.domain.DomainError
import hr.kbratko.instakt.domain.model.SocialMediaLink
import hr.kbratko.instakt.domain.model.User

interface SocialMediaLinkPersistence {
    suspend fun insert(link: SocialMediaLink.New): Either<DomainError, SocialMediaLink>

    suspend fun select(userId: User.Id): Set<SocialMediaLink>

    suspend fun update(data: SocialMediaLink.Edit): Either<DomainError, SocialMediaLink>

    suspend fun delete(data: SocialMediaLink.Delete): Either<DomainError, SocialMediaLink.Id>
}