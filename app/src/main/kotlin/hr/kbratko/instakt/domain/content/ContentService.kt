package hr.kbratko.instakt.domain.content

import arrow.core.Either
import arrow.core.EitherNel
import arrow.core.Nel
import arrow.core.nel
import arrow.core.raise.Raise
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.toEitherNel
import hr.kbratko.instakt.domain.DbError.MaximumStorageForPlanExceeded
import hr.kbratko.instakt.domain.DbError.UnsupportedContentType
import hr.kbratko.instakt.domain.DbError.UserNotFound
import hr.kbratko.instakt.domain.DomainError
import hr.kbratko.instakt.domain.content.ContentService.Metadata
import hr.kbratko.instakt.domain.eitherNel
import hr.kbratko.instakt.domain.getOrRaiseNel
import hr.kbratko.instakt.domain.model.Content
import hr.kbratko.instakt.domain.model.ContentMetadata
import hr.kbratko.instakt.domain.model.User
import hr.kbratko.instakt.domain.persistence.ContentMetadataPersistence
import hr.kbratko.instakt.domain.persistence.ContentPersistence
import hr.kbratko.instakt.domain.persistence.UserPersistence
import hr.kbratko.instakt.domain.toEitherNel
import hr.kbratko.instakt.domain.validation.ContentSizeIsValid
import hr.kbratko.instakt.domain.validation.validate
import java.io.InputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface ContentService {
    suspend fun upload(
        userId: User.Id,
        imageStream: InputStream,
        metadata: Metadata
    ): EitherNel<DomainError, ContentMetadata>

    suspend fun uploadProfilePhoto(userId: User.Id, imageStream: InputStream): EitherNel<DomainError, ContentMetadata>

    suspend fun delete(userId: User.Id, contentMetadataId: ContentMetadata.Id): Either<DomainError, Unit>

    suspend fun deleteProfilePhoto(userId: User.Id): Either<DomainError, Unit>

    data class Metadata(
        val description: ContentMetadata.Description,
        val tags: List<ContentMetadata.Tag>
    )
}

fun ContentService(
    userPersistence: UserPersistence,
    contentTypeDetector: ContentTypeDetector,
    contentPersistence: ContentPersistence,
    contentMetadataPersistence: ContentMetadataPersistence
) = object : ContentService {
    override suspend fun upload(
        userId: User.Id,
        imageStream: InputStream,
        metadata: Metadata
    ): EitherNel<DomainError, ContentMetadata> = eitherNel {
        val (contentType, contentValue) = getContent(imageStream).bind()

        validateAccordingToPlan(userId, contentValue)

        val content = contentPersistence.upload(
            Content.New(
                userId,
                contentType,
                contentValue
            )
        ).toEitherNel().bind()

        contentMetadataPersistence.insert(
            ContentMetadata.New(
                userId,
                content.id,
                content.type,
                ContentMetadata.SizeInBytes(content.value.size),
                metadata.description,
                metadata.tags
            )
        )
    }

    override suspend fun delete(userId: User.Id, contentMetadataId: ContentMetadata.Id): Either<DomainError, Unit> =
        either {
            val contentId = contentMetadataPersistence.delete(
                ContentMetadata.Delete(
                    contentMetadataId,
                    userId
                )
            ).bind()

            contentPersistence.remove(contentId).bind()
        }

    override suspend fun uploadProfilePhoto(
        userId: User.Id,
        imageStream: InputStream
    ): EitherNel<DomainError, ContentMetadata> = eitherNel {
        val (contentType, contentValue) = getContent(imageStream).bind()

        contentMetadataPersistence.selectProfile(userId)
            .onSome {
                contentPersistence.remove(it.url).toEitherNel().bind()
            }

        val content = contentPersistence.upload(
            Content.New(
                userId,
                contentType,
                contentValue
            )
        ).toEitherNel().bind()

        contentMetadataPersistence.insert(
            ContentMetadata.NewProfile(
                userId,
                content.id,
                content.type,
                ContentMetadata.SizeInBytes(content.value.size),
            )
        ).toEitherNel().bind()
    }

    override suspend fun deleteProfilePhoto(
        userId: User.Id
    ): Either<DomainError, Unit> = either {
        val contentId = contentMetadataPersistence.delete(
            ContentMetadata.DeleteProfile(userId)
        ).bind()

        contentPersistence.remove(contentId).bind()
    }

    suspend fun getContent(imageStream: InputStream): EitherNel<DomainError, Pair<Content.Type, ByteArray>> =
        either {
            val contentType = contentTypeDetector.detect(imageStream)
                .toEitherNel { UnsupportedContentType }.bind()
            val contentValue = withContext(Dispatchers.IO) { imageStream.readBytes() }
                .also { it.validate(ContentSizeIsValid).bind() }

            contentType to contentValue
        }

    private suspend fun Raise<Nel<DomainError>>.validateAccordingToPlan(
        userId: User.Id,
        contentValue: ByteArray
    ) {
        val plan = userPersistence.selectPlan(userId).getOrRaiseNel { UserNotFound }
        val totalUploadedBytes = contentMetadataPersistence.sumTotalUploadedBytes(userId)
        val bytesAfterUpload = totalUploadedBytes + contentValue.size
        println("Plan: $plan, totalUploadedBytes: $totalUploadedBytes, bytesAfterUpload: $bytesAfterUpload")

        ensure(plan.maxStorageInBytes >= bytesAfterUpload) { MaximumStorageForPlanExceeded.nel() }
    }

}