package hr.kbratko.instakt.infrastructure.persistence.exposed

import arrow.core.Either
import arrow.core.Either.Companion.catchOrThrow
import arrow.core.raise.either
import arrow.core.raise.ensure
import hr.kbratko.instakt.domain.DbError.SocialMediaLinkForPlatformAlreadyExists
import hr.kbratko.instakt.domain.DbError.SocialMediaLinkNotFound
import hr.kbratko.instakt.domain.DomainError
import hr.kbratko.instakt.domain.conversion.convert
import hr.kbratko.instakt.domain.model.SocialMediaLink
import hr.kbratko.instakt.domain.model.User
import hr.kbratko.instakt.domain.persistence.SocialMediaLinkPersistence
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.postgresql.util.PSQLState.UNIQUE_VIOLATION
import org.jetbrains.exposed.sql.ReferenceOption.CASCADE as Cascade
import org.jetbrains.exposed.sql.SchemaUtils.create as createIfNotExists

private const val USER_PLATFORM_UNIQUE_INDEX = "social_media_links_user_fk_platform_unique_index"

object SocialMediaLinksTable : LongIdTable("social_media_links", "social_media_link_pk") {
    val userId = reference("user_fk", UsersTable, onDelete = Cascade)
    val platform = varchar("platform", 100)
    val url = varchar("url", 256)
}

val linkSelection = TableSelection(
    SocialMediaLinksTable.id,
    SocialMediaLinksTable.platform,
    SocialMediaLinksTable.url
) {
    SocialMediaLink(
        SocialMediaLink.Id(this[SocialMediaLinksTable.id].value),
        SocialMediaLink.Platform(this[SocialMediaLinksTable.platform]),
        SocialMediaLink.Url(this[SocialMediaLinksTable.url])
    )
}

fun ExposedSocialMediaLinkPersistence(db: Database) =
    object : SocialMediaLinkPersistence {
        init {
            transaction {
                createIfNotExists(SocialMediaLinksTable)
            }
        }

        override suspend fun insert(link: SocialMediaLink.New): Either<DomainError, SocialMediaLink> = either {
            ioTransaction(db = db) {
                val id = catchOrThrow<ExposedSQLException, EntityID<Long>> {
                    SocialMediaLinksTable.insertAndGetId {
                        it[userId] = link.userId.value
                        it[platform] = link.platform.value
                        it[url] = link.url.value
                    }
                }.mapLeft { it.convert(ExposedSQLExceptionToDbErrorConversion) }.bind()

                SocialMediaLinksTable
                    .select(linkSelection.columns)
                    .where { SocialMediaLinksTable.id eq id }
                    .single()
                    .convert(linkSelection.conversion)
            }
        }

        override suspend fun select(userId: User.Id): Set<SocialMediaLink> = ioTransaction(db = db) {
            SocialMediaLinksTable
                .select(linkSelection.columns)
                .where { SocialMediaLinksTable.userId eq userId.value }
                .map { it.convert(linkSelection.conversion) }
                .toSet()
        }

        override suspend fun update(data: SocialMediaLink.Edit): Either<DomainError, SocialMediaLink> = either {
            ioTransaction(db = db) {
                val updatedCount = catchOrThrow<ExposedSQLException, Int> {
                    SocialMediaLinksTable.update({
                        (SocialMediaLinksTable.id eq data.id.value) and
                        (SocialMediaLinksTable.userId eq data.userId.value )
                    }) {
                        it[platform] = data.platform.value
                        it[url] = data.url.value
                    }
                }.mapLeft { it.convert(ExposedSQLExceptionToDbErrorConversion) }.bind()

                ensure(updatedCount > 0) { SocialMediaLinkNotFound }

                SocialMediaLinksTable
                    .select(linkSelection.columns)
                    .where { SocialMediaLinksTable.id eq data.id.value }
                    .single()
                    .convert(linkSelection.conversion)
            }
        }

        override suspend fun delete(data: SocialMediaLink.Delete): Either<DomainError, SocialMediaLink.Id> = either {
            ioTransaction(db = db) {
                val deletedCount = SocialMediaLinksTable.deleteWhere {
                    (id eq data.id.value) and
                    (userId eq data.userId.value )
                }

                ensure(deletedCount > 0) { SocialMediaLinkNotFound }

                data.id
            }
        }
    }

private val ExposedSQLExceptionToDbErrorConversion = ExposedSQLExceptionToDomainErrorConversionScope {
    when (sqlState) {
        UNIQUE_VIOLATION.state -> {
            val message = message?.lowercase() ?: ""
            when {
                message.contains(USER_PLATFORM_UNIQUE_INDEX) -> SocialMediaLinkForPlatformAlreadyExists
                else -> error("UserPersistence SQLException caught: ${this.message ?: ""}")
            }
        }

        else -> error("SocialMediaLinksPersistence SQLException caught: ${message ?: ""}")
    }
}