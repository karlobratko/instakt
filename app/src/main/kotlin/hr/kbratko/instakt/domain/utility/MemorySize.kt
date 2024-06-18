package hr.kbratko.instakt.domain.utility

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind.LONG
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

private const val BYTE = 1L
private const val KILOBYTE = 1024 * BYTE
private const val MEGABYTE = 1024 * KILOBYTE
private const val GIGABYTE = 1024 * MEGABYTE
private const val TERABYTE = 1024 * GIGABYTE
private const val PETABYTE = 1024 * TERABYTE

@Serializable(with = MemorySizeSerializer::class)
data class MemorySize(val bytes: Long) : Comparable<MemorySize> {

    val kilobytes get() = bytes / KILOBYTE
    val megabytes get() = bytes / MEGABYTE
    val gigabytes get() = bytes / GIGABYTE
    val terabytes get() = bytes / TERABYTE
    val petabytes get() = bytes / PETABYTE

    operator fun plus(other: MemorySize) = MemorySize(bytes + other.bytes)

    operator fun minus(other: MemorySize) = MemorySize(bytes - other.bytes)

    operator fun times(factor: Long) = MemorySize(bytes * factor)

    operator fun div(divisor: Long) = MemorySize(bytes / divisor)

    override fun compareTo(other: MemorySize): Int = bytes.compareTo(other.bytes)

    override fun toString(): String {
        return when {
            bytes < KILOBYTE -> "$bytes B"
            bytes < MEGABYTE -> "$kilobytes KB"
            bytes < GIGABYTE -> "$megabytes MB"
            bytes < TERABYTE -> "$gigabytes GB"
            bytes < PETABYTE -> "$terabytes TB"
            else -> "$petabytes PB"
        }
    }
}

val Int.bytes: MemorySize
    get() = MemorySize(this * BYTE)

val Int.kilobytes: MemorySize
    get() = MemorySize(this * KILOBYTE)

val Int.megabytes: MemorySize
    get() = MemorySize(this * MEGABYTE)

val Int.gigabytes: MemorySize
    get() = MemorySize(this * GIGABYTE)

val Int.terabytes: MemorySize
    get() = MemorySize(this * TERABYTE)

val Int.petabytes: MemorySize
    get() = MemorySize(this * PETABYTE)

val Long.bytes: MemorySize
    get() = MemorySize(this)

val Long.kilobytes: MemorySize
    get() = MemorySize(this * KILOBYTE)

val Long.megabytes: MemorySize
    get() = MemorySize(this * MEGABYTE)

val Long.gigabytes: MemorySize
    get() = MemorySize(this * GIGABYTE)

val Long.terabytes: MemorySize
    get() = MemorySize(this * TERABYTE)

val Long.petabytes: MemorySize
    get() = MemorySize(this * PETABYTE)

object MemorySizeSerializer : KSerializer<MemorySize> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("MemorySize", LONG)

    override fun serialize(encoder: Encoder, value: MemorySize) {
        encoder.encodeLong(value.bytes)
    }

    override fun deserialize(decoder: Decoder): MemorySize {
        val bytes = decoder.decodeLong()
        return MemorySize(bytes)
    }
}