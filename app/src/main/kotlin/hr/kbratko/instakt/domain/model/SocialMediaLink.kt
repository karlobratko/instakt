package hr.kbratko.instakt.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class SocialMediaLink(
    val id: Id,
    val platform: Platform,
    val url: Url
) {
    data class New(
        val userId: User.Id,
        val platform: Platform,
        val url: Url
    )

    data class Edit(
        val id: Id,
        val platform: Platform,
        val url: Url
    )

    @Serializable @JvmInline value class Id(val value: Long)

    @Serializable @JvmInline value class Platform(val value: String)

    @Serializable @JvmInline value class Url(val value: String)
}