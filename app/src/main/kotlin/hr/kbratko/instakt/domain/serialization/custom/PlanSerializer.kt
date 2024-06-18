package hr.kbratko.instakt.domain.serialization.custom

import hr.kbratko.instakt.domain.model.Plan
import hr.kbratko.instakt.domain.model.Plan.entries
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

object PlanSerializer : KSerializer<Plan> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Plan") {
        element<String>("name")
        element<Int>("maxStorageInMegabytes")
    }

    override fun serialize(encoder: Encoder, value: Plan) {
        val jsonEncoder = encoder as? JsonEncoder
            ?: throw SerializationException("This class can be saved only by JSON")

        val jsonObject = JsonObject(
            mapOf(
                "name" to JsonPrimitive(value.name.lowercase()),
                "maxStorageInMegabytes" to JsonPrimitive(value.maxStorageInMegabytes)
            )
        )

        jsonEncoder.encodeJsonElement(jsonObject)
    }

    override fun deserialize(decoder: Decoder): Plan {
        val jsonDecoder = decoder as? JsonDecoder
            ?: throw SerializationException("This class can be loaded only by JSON")

        val jsonElement = jsonDecoder.decodeJsonElement()

        return if (jsonElement is JsonPrimitive && jsonElement.isString) {
            entries.find { it.name.lowercase() == jsonElement.content }
                ?: throw SerializationException("Unsupported plan name")
        } else {
            throw SerializationException("Unsupported deserialization format")
        }
    }
}