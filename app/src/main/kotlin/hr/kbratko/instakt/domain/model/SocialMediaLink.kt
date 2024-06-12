package hr.kbratko.instakt.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class SocialMediaLink(
    val id: Id,
    val platform: Platform,
    val url: Url
) {
    class New(
        val userId: User.Id,
        val platform: Platform,
        val url: Url
    )

    class Edit(
        val id: Id,
        val userId: User.Id,
        val platform: Platform,
        val url: Url
    )

    class Delete(
        val id: Id,
        val userId: User.Id,
    )

    @Serializable @JvmInline value class Id(val value: Long)

    @Serializable @JvmInline value class Platform(val value: String)

    @Serializable @JvmInline value class Url(val value: String)
}