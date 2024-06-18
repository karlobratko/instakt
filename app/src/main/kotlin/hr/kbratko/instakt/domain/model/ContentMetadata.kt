package hr.kbratko.instakt.domain.model

import arrow.core.Nel
import arrow.core.Option
import hr.kbratko.instakt.domain.utility.InstantClosedRange
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class ContentMetadata(
    val id: Id,
    val user: User,
    val url: Content.Id,
    val type: Content.Type,
    val description: Description,
    val uploadedAt: Instant,
    val tags: List<Tag>
) {
    class New(
        val userId: User.Id,
        val contentId: Content.Id,
        val type: Content.Type,
        val size: SizeInBytes,
        val description: Description,
        val tags: List<Tag>
    )

    class NewProfile(
        val userId: User.Id,
        val contentId: Content.Id,
        val type: Content.Type,
        val size: SizeInBytes,
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

    class Filter(
        val username: Option<User.Username>,
        val description: Option<Description>,
        val uploadedBetween: Option<InstantClosedRange>,
        val tags: Option<Nel<Tag>>,
    )

    @Serializable @JvmInline value class Id(val value: String)

    @Serializable @JvmInline value class Description(val value: String = "")

    @Serializable @JvmInline value class SizeInBytes(val value: Int)

    @Serializable @JvmInline value class Tag(val value: String)
}
