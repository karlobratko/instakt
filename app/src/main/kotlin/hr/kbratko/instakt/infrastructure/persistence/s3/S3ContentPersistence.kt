package hr.kbratko.instakt.infrastructure.persistence.s3

import arrow.core.Either
import arrow.core.Either.Companion.catchOrThrow
import arrow.core.Option
import arrow.core.raise.nullable
import arrow.core.toOption
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.DeleteObjectRequest
import aws.sdk.kotlin.services.s3.model.GetObjectRequest
import aws.sdk.kotlin.services.s3.model.PutObjectRequest
import aws.smithy.kotlin.runtime.content.ByteStream
import aws.smithy.kotlin.runtime.content.toByteArray
import hr.kbratko.instakt.domain.DbError.CouldNotDeleteContent
import hr.kbratko.instakt.domain.DbError.CouldNotPersistContent
import hr.kbratko.instakt.domain.DomainError
import hr.kbratko.instakt.domain.config.DefaultInstantProvider
import hr.kbratko.instakt.domain.model.Content
import hr.kbratko.instakt.domain.model.Content.Id
import hr.kbratko.instakt.domain.model.User
import hr.kbratko.instakt.domain.persistence.ContentPersistence
import hr.kbratko.instakt.domain.toLocalDate
import java.util.UUID
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone.Companion.UTC

data class ContentPersistenceConfig(val bucket: Content.Bucket)

fun S3ContentPersistence(client: S3Client, config: ContentPersistenceConfig) =
    object : ContentPersistence {
        override suspend fun upload(content: Content.New): Either<DomainError, Content> {
            val uploadDate = DefaultInstantProvider.now().toLocalDate(UTC)
            val id = generateKey(content.userId, uploadDate)

            return catchOrThrow<Exception, Content> {
                client.putObject(PutObjectRequest {
                    bucket = config.bucket.value
                    key = id.value
                    body = ByteStream.fromBytes(content.value)
                    contentType = content.type.name
                })

                Content(id, content.type, content.value)
            }.mapLeft { CouldNotPersistContent }
        }

        override suspend fun download(id: Id): Option<Content> =
            client.getObject(GetObjectRequest {
                bucket = config.bucket.value
                key = id.value
            }) {
                nullable {
                    Content(
                        id,
                        Content.Type.valueOf(it.contentType.bind()),
                        it.body.bind().toByteArray()
                    )
                }.toOption()
            }

        override suspend fun remove(id: Id): Either<DomainError, Id> =
            catchOrThrow<Exception, Id> {
                client.deleteObject(DeleteObjectRequest {
                    bucket = config.bucket.value
                    key = id.value
                })

                id
            }.mapLeft { CouldNotDeleteContent }

        private fun generateKey(userId: User.Id, uploadDate: LocalDate) =
            Id("/${userId.value}/${uploadDate.year}/${uploadDate.month.value}/${uploadDate.dayOfMonth}/${UUID.randomUUID()}")
    }