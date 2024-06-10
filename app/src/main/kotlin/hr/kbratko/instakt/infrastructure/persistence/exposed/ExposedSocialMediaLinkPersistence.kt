package hr.kbratko.instakt.infrastructure.persistence.exposed

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.ReferenceOption.CASCADE

private const val USER_PLATFORM_UNIQUE_INDEX = "social_media_links_user_fk_platform_unique_index"

object SocialMediaLinksTable : LongIdTable("social_media_link_pk") {
    val userId = reference("user_fk", UsersTable, onDelete = CASCADE)
    val platform = varchar("platform", 100)
    val url = varchar("url", 256)
}
