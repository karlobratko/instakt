package hr.kbratko.instakt.domain.model

import arrow.core.Option
import hr.kbratko.instakt.domain.utility.InstantClosedRange
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class AuditLog(
    val id: Id,
    val userId: User.Id,
    val action: Action,
    val affectedResource: Resource,
    val executedAt: Instant
) {
    class New(
        val userId: User.Id,
        val action: Action,
        val affectedResource: Resource
    )

    class Filter(
        val userId: Option<User.Id>,
        val action: Option<Action>,
        val affectedResource: Option<Resource>,
        val executedBetween: Option<InstantClosedRange>
    )

    @Serializable @JvmInline value class Id(val value: String)

    @Serializable
    enum class Action {
        Register,
        Login,
        Reset,
        Create,
        Update,
        Delete
    }

    @Serializable
    enum class Resource {
        Content,
        Password,
        User,
        ProfilePicture,
        Plan,
        SocialMediaLink
    }
}