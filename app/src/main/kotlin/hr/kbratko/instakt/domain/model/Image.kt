package hr.kbratko.instakt.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Image(val id: Id) {
    @Serializable @JvmInline value class Id(val value: String)

    enum class ContentType() {
        jpeg,
        png,
        webp,
        avif,
        heif,
        heic,
        svg
    }
}
