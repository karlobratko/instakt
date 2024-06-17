package hr.kbratko.instakt.infrastructure.persistence.exposed

import arrow.core.Either
import arrow.core.Option
import arrow.core.getOrElse
import arrow.core.raise.Raise
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.singleOrNone
import arrow.core.some
import arrow.core.toOption
import hr.kbratko.instakt.domain.DbError.ContentMetadataNotFound
import hr.kbratko.instakt.domain.DbError.ProfilePictureMetadataNotFound
import hr.kbratko.instakt.domain.DbError.UserNotFound
import hr.kbratko.instakt.domain.DomainError
import hr.kbratko.instakt.domain.conversion.ConversionScope
import hr.kbratko.instakt.domain.conversion.convert
import hr.kbratko.instakt.domain.getOrRaise
import hr.kbratko.instakt.domain.model.Content
import hr.kbratko.instakt.domain.model.ContentMetadata
import hr.kbratko.instakt.domain.model.User
import hr.kbratko.instakt.domain.persistence.ContentMetadataPersistence
import hr.kbratko.instakt.domain.persistence.pagination.Page
import hr.kbratko.instakt.domain.persistence.pagination.Sort
import hr.kbratko.instakt.domain.toKotlinInstant
import hr.kbratko.instakt.domain.toUUIDOrNone
import hr.kbratko.instakt.infrastructure.persistence.exposed.pagination.toOrderedExpressions
import java.time.ZoneOffset.UTC
import java.util.UUID
import kotlinx.datetime.toJavaInstant
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.innerJoin
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentTimestamp
import org.jetbrains.exposed.sql.kotlin.datetime.timestampWithTimeZone
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.ReferenceOption.CASCADE as Cascade
import org.jetbrains.exposed.sql.SchemaUtils.create as createIfNotExists

object ContentMetadataTable : UUIDTable("content_metadata", "content_metadata_pk") {
    val userId = reference("user_fk", UsersTable, onDelete = Cascade)
    val path = varchar("path", 256)
    val contentType = enumeration<Content.Type>("content_type")
    val sizeInBytes = integer("size_bytes")
    val description = varchar("description", 1024).default("")
    val uploadedAt = timestampWithTimeZone("uploaded_at").defaultExpression(CurrentTimestamp())
}

object TagsTable : LongIdTable("tags", "tag_pk") {
    val name = varchar("name", 50)
    val contentMetadataId = reference("content_metadata_fk", ContentMetadataTable, onDelete = Cascade)
}

val metadataSelection = TableSelection(
    listOf(
        ContentMetadataTable.id,
        ContentMetadataTable.userId,
        ContentMetadataTable.path,
        ContentMetadataTable.contentType,
        ContentMetadataTable.sizeInBytes,
        ContentMetadataTable.description,
        ContentMetadataTable.uploadedAt
    ) + userSelection.columns
) {
    ContentMetadata(
        ContentMetadata.Id(this[ContentMetadataTable.id].value.toString()),
        this.convert(userSelection.conversion),
        Content.Id(this[ContentMetadataTable.path]),
        this[ContentMetadataTable.contentType],
        ContentMetadata.Description(this[ContentMetadataTable.description]),
        this[ContentMetadataTable.uploadedAt].toKotlinInstant(),
        emptyList()
    )
}

val tagSelection = TableSelection(TagsTable.name) { ContentMetadata.Tag(this[TagsTable.name]) }

fun ExposedContentMetadataPersistence(db: Database) =
    object : ContentMetadataPersistence {
        init {
            transaction {
                createIfNotExists(ContentMetadataTable)
            }
        }

        override suspend fun insert(metadata: ContentMetadata.New): ContentMetadata = ioTransaction(db = db) {
            val contentMetadataId = ContentMetadataTable.insertAndGetId {
                it[userId] = metadata.userId.value
                it[path] = metadata.contentId.value
                it[contentType] = metadata.type
                it[sizeInBytes] = metadata.size.value
                it[description] = metadata.description.value
            }

            val tags = insertTags(contentMetadataId.value, metadata.tags)

            forceSelectMetadata(contentMetadataId.value).copy(tags = tags)
        }

        override suspend fun insert(metadata: ContentMetadata.NewProfile): Either<DomainError, ContentMetadata> =
            either {
                ioTransaction(db = db) {
                    selectUserProfilePhotoId(metadata.userId).bind()
                        .fold(
                            ifEmpty = { insertAndUpdateUser(metadata) },
                            ifSome = { profilePictureId ->
                                insertAndUpdateUser(metadata).also {
                                    ContentMetadataTable.deleteWhere {
                                        id eq profilePictureId
                                    }
                                }
                            }
                        )
                        .let { id -> forceSelectMetadata(id.value) }
                }
            }

        override suspend fun select(
            filter: ContentMetadata.Filter,
            page: Page,
            sort: Option<Sort>
        ): Set<ContentMetadata> =
            ioTransaction(db = db) {
                val metadataIds = mutableListOf<EntityID<UUID>>()
                val metadata = ContentMetadataTable
                    .innerJoin(UsersTable, { userId }, { id })
                    .select(metadataSelection.columns)
                    .apply {
                        filter.username.onSome {
                            andWhere { UsersTable.username like "%${it.value}%" }
                        }
                        filter.description.onSome {
                            andWhere { ContentMetadataTable.description like "%${it.value}%" }
                        }
                        filter.uploadedAtBetween.onSome {
                            andWhere {
                                ContentMetadataTable.uploadedAt.between(
                                    it.start.toJavaInstant().atOffset(UTC),
                                    it.endInclusive.toJavaInstant().atOffset(UTC)
                                )
                            }
                        }
                        filter.tags.onSome { tags ->
                            andWhere {
                                ContentMetadataTable.id.inSubQuery(
                                    TagsTable
                                        .select(TagsTable.contentMetadataId)
                                        .where { TagsTable.name inList tags.map { it.value } }
                                )
                            }
                        }
                    }
                    .orderBy(
                        *sort.getOrElse { Sort("id") }
                            .toOrderedExpressions(ColumnNameToColumnConversion)
                            .toTypedArray()
                    )
                    .limit(page.count, page.offset)
                    .distinct()
                    .map { row ->
                        metadataIds += row[ContentMetadataTable.id]
                        row.convert(metadataSelection.conversion)
                    }

                val tags = TagsTable
                    .select(tagSelection.columns + TagsTable.contentMetadataId)
                    .where { TagsTable.contentMetadataId inList metadataIds }
                    .groupBy(
                        { ContentMetadata.Id(it[TagsTable.contentMetadataId].value.toString()) },
                        { it.convert(tagSelection.conversion) }
                    )

                metadata.map { it.copy(tags = tags[it.id]!!) }.toSet()
            }

        override suspend fun selectProfile(userId: User.Id): Option<ContentMetadata> = ioTransaction(db = db) {
            ContentMetadataTable.innerJoin(UsersTable, onColumn = { this.id }, otherColumn = { this.profilePictureId })
                .select(metadataSelection.columns)
                .where {
                    (ContentMetadataTable.userId eq userId.value) and
                            (ContentMetadataTable.id eq UsersTable.profilePictureId)
                }
                .singleOrNone()
                .map { it.convert(metadataSelection.conversion) }
        }

        override suspend fun selectNonProfile(userId: User.Id): Set<ContentMetadata> = ioTransaction(db = db) {
            val notProfilePicture = selectUserProfilePhotoId(userId)
                .fold(
                    ifLeft = { return@ioTransaction emptySet() },
                    ifRight = {
                        it.fold(
                            ifEmpty = { (ContentMetadataTable.userId eq userId.value) },
                            ifSome = {
                                Op.build {
                                    (ContentMetadataTable.userId eq userId.value) and (ContentMetadataTable.id neq it)
                                }
                            }
                        )
                    }
                )

            val tags = ContentMetadataTable
                .innerJoin(
                    TagsTable,
                    onColumn = { this.id },
                    otherColumn = { this.contentMetadataId }
                )
                .select(tagSelection.columns + ContentMetadataTable.id)
                .where { notProfilePicture }
                .groupBy(
                    { ContentMetadata.Id(it[ContentMetadataTable.id].value.toString()) },
                    { it.convert(tagSelection.conversion) }
                )

            (UsersTable innerJoin ContentMetadataTable)
                .select(metadataSelection.columns)
                .where { notProfilePicture }
                .map { row ->
                    row.convert(metadataSelection.conversion).let { it.copy(tags = tags[it.id]!!) }
                }
                .toSet()
        }

        override suspend fun update(data: ContentMetadata.Edit): Either<DomainError, ContentMetadata> = either {
            ioTransaction(db = db) {
                val contentMetadataId = toUUID(data.id)

                ContentMetadataTable
                    .update({
                        (ContentMetadataTable.id eq contentMetadataId) and
                                (ContentMetadataTable.userId eq data.userId.value)
                    }) { it[description] = data.description.value }
                    .also { updatedCount -> ensure(updatedCount > 0) { ContentMetadataNotFound } }

                val tags = updateTags(contentMetadataId, data.tags)

                forceSelectMetadata(contentMetadataId).copy(tags = tags)
            }
        }

        override suspend fun delete(data: ContentMetadata.Delete): Either<DomainError, Content.Id> = either {
            ioTransaction(db = db) {
                val contentMetadataId = toUUID(data.id)
                deleteMetadataWithTags(contentMetadataId, data.userId)
            }
        }

        override suspend fun delete(data: ContentMetadata.DeleteProfile): Either<DomainError, Content.Id> = either {
            ioTransaction(db = db) {
                selectUserProfilePhotoId(data.userId).bind()
                    .fold(
                        ifEmpty = { raise(ProfilePictureMetadataNotFound) },
                        ifSome = { contentMetadataId ->
                            UsersTable
                                .update({ UsersTable.id eq data.userId.value }) {
                                    it[profilePictureId] = null
                                }
                                .also { updatedCount -> ensure(updatedCount > 0) { UserNotFound } }

                            deleteMetadataWithTags(contentMetadataId, data.userId)
                        }
                    )
            }
        }

        private fun forceSelectMetadata(id: UUID) =
            (UsersTable innerJoin ContentMetadataTable)
                .select(metadataSelection.columns)
                .where { ContentMetadataTable.id eq id }
                .single()
                .convert(metadataSelection.conversion)

        private fun selectUserProfilePhotoId(userId: User.Id) =
            UsersTable
                .select(UsersTable.profilePictureId)
                .where { UsersTable.id eq userId.value }
                .singleOrNone()
                .toEither { UserNotFound }
                .map { row -> row[UsersTable.profilePictureId].toOption().map { it.value } }

        private fun insertAndUpdateUser(metadata: ContentMetadata.NewProfile): EntityID<UUID> =
            ContentMetadataTable
                .insertAndGetId {
                    it[userId] = metadata.userId.value
                    it[path] = metadata.contentId.value
                    it[contentType] = metadata.type
                    it[sizeInBytes] = metadata.size.value
                }
                .also { id ->
                    UsersTable.update({ UsersTable.id eq metadata.userId.value }) {
                        it[profilePictureId] = id
                    }
                }

        private fun insertTags(
            contentMetadataId: UUID,
            tags: List<ContentMetadata.Tag>
        ): List<ContentMetadata.Tag> =
            TagsTable
                .batchInsert(tags.toSet()) {
                    this[TagsTable.name] = it.value
                    this[TagsTable.contentMetadataId] = contentMetadataId
                }
                .map { it.convert(tagSelection.conversion) }

        private fun Raise<DomainError>.toUUID(id: ContentMetadata.Id) =
            id.value.toUUIDOrNone().getOrRaise { ContentMetadataNotFound }

        private fun Raise<DomainError>.deleteMetadata(contentMetadataId: UUID, userId: User.Id) =
            ContentMetadataTable
                .select(ContentMetadataTable.path)
                .where { ContentMetadataTable.id eq contentMetadataId }
                .singleOrNone()
                .map { Content.Id(it[ContentMetadataTable.path]) }
                .getOrRaise { ContentMetadataNotFound }
                .also {
                    ContentMetadataTable
                        .deleteWhere {
                            (id eq contentMetadataId) and
                                    (this.userId eq userId.value)
                        }
                        .also { deletedCount -> ensure(deletedCount > 0) { ContentMetadataNotFound } }

                }

        private fun deleteTags(contentMetadataId: UUID) {
            TagsTable.deleteWhere { (this.contentMetadataId eq contentMetadataId) }
        }

        private fun Raise<DomainError>.deleteMetadataWithTags(contentMetadataId: UUID, userId: User.Id) =
            deleteMetadata(contentMetadataId, userId).also {
                deleteTags(contentMetadataId)
            }

        private fun updateTags(
            contentMetadataId: UUID,
            tags: List<ContentMetadata.Tag>
        ): List<ContentMetadata.Tag> {
            deleteTags(contentMetadataId)
            return insertTags(contentMetadataId, tags)
        }
    }

private val ColumnNameToColumnConversion = ConversionScope<String, Option<Column<*>>> {
    when (this) {
        "id" -> ContentMetadataTable.id.some()
        "userId" -> ContentMetadataTable.userId.some()
        "path" -> ContentMetadataTable.path.some()
        "description" -> ContentMetadataTable.description.some()
        "uploadedAt" -> ContentMetadataTable.uploadedAt.some()
        else -> arrow.core.none()
    }
}