package hr.kbratko.instakt.infrastructure.persistence.exposed

import hr.kbratko.instakt.domain.model.Image
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentTimestamp
import org.jetbrains.exposed.sql.kotlin.datetime.timestampWithTimeZone
import org.jetbrains.exposed.sql.ReferenceOption.CASCADE as Cascade

private const val BUCKET_KEY_UNIQUE_INDEX = "images_bucket_key_unique_index"

object ImagesTable : UUIDTable("images", "image_pk") {
    val userId = reference("user_fk", UsersTable, onDelete = Cascade)
    val bucket = uuid("bucket")
    val key = varchar("key", 256)
    val contentType = enumeration<Image.ContentType>("content_type")
    val description = varchar("description", 1024)
    val uploadTime = timestampWithTimeZone("upload_time").defaultExpression(CurrentTimestamp())

    val bucketKeyUniqueIndex = uniqueIndex(BUCKET_KEY_UNIQUE_INDEX, bucket, key)
}

object TagsTable : LongIdTable("tag_pk") {
    val name = varchar("name", 50)
    val imageId = reference("image_fk", ImagesTable, onDelete = Cascade)
}