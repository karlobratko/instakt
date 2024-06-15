package hr.kbratko.instakt.domain.serialization.custom

import hr.kbratko.instakt.domain.model.User
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure

object UserSerializer : KSerializer<User> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("User") {
        element<Long>("id")
        element<String>("username")
        element<String>("email")
    }

    override fun serialize(encoder: Encoder, value: User) {
        encoder.encodeStructure(descriptor) {
            encodeLongElement(descriptor, 0, value.id.value)
            encodeStringElement(descriptor, 1, value.username.value)
            encodeStringElement(descriptor, 2, value.email.value)
        }
    }

    override fun deserialize(decoder: Decoder): User {
        return decoder.decodeStructure(descriptor) {
            var id: Long? = null
            var username: String? = null
            var email: String? = null

            loop@ while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    CompositeDecoder.DECODE_DONE -> break@loop
                    0 -> id = decodeLongElement(descriptor, 0)
                    1 -> username = decodeStringElement(descriptor, 1)
                    2 -> email = decodeStringElement(descriptor, 2)
                    else -> throw SerializationException("Unknown index $index")
                }
            }

            User(
                User.Id(id ?: throw SerializationException("Id missing")),
                User.Username(username ?: throw SerializationException("Username missing")),
                User.Email(email ?: throw SerializationException("Email missing"))
            )
        }
    }
}