package hr.kbratko.instakt.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

data class Content(
    val id: Id,
    val type: Type,
    val value: ByteArray
) {
    class New(
        val userId: User.Id,
        val type: Type,
        val value: ByteArray
    )

    @Serializable @JvmInline value class Id(val value: String)
    @JvmInline value class Bucket(val value: String)


    @Serializable
    enum class Type(val value: String) {
        @SerialName("image/jpeg") jpeg("image/jpeg"),
        @SerialName("image/png") png("image/png"),
//        webp("image/webp"),
//        avif("image/avif"),
//        heif("image/heif"),
//        heic("image/heic"),
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Content

        if (id != other.id) return false
        if (type != other.type) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + type.hashCode()
        return result
    }
}