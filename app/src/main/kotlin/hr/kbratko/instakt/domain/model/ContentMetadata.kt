package hr.kbratko.instakt.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class ContentMetadata(
    val id: Id,
    val user: User,
    val url: Content.Id,
    val description: Description,
    val uploadedAt: Instant,
    val tags: List<Tag>
) {
    class New(
        val userId: User.Id,
        val contentId: Content.Id,
        val description: Description,
        val tags: List<Tag>
    )

    class NewProfile(
        val userId: User.Id,
        val contentId: Content.Id
    )

    class Edit(
        val id: Id,
        val userId: User.Id,
        val description: Description,
        val tags: List<Tag>
    )

    class Delete(
        val id: Id,
        val userId: User.Id,
    )

    class DeleteProfile(
        val userId: User.Id,
    )

    @Serializable @JvmInline value class Id(val value: String)

    @Serializable @JvmInline value class Description(val value: String = "")

    @Serializable @JvmInline value class Tag(val value: String)
}
