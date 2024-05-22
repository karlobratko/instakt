package hr.kbratko.instakt.infrastructure.serialization

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.serializer

/**
 * Decodes a JSON resource file into an object of type T using a provided deserialization strategy.
 *
 * This function is used to read a JSON file from the resources directory and convert it into an object of type T.
 * The conversion is done using the provided deserialization strategy.
 *
 * @param deserializer The deserialization strategy to be used for converting the JSON file into an object of type T.
 * @param path The path to the JSON resource file. The path should be relative to the `resources` directory.
 *
 * @return The object of type T obtained by decoding the JSON resource file.
 *
 * @throws NoSuchElementException If the resource file at the specified path does not exist.
 */
@OptIn(ExperimentalSerializationApi::class)
fun <T> Json.decodeFromResource(deserializer: DeserializationStrategy<T>, path: String): T =
    decodeFromStream(
        deserializer,
        this::class.java.classLoader.getResourceAsStream(path)!!
    )

/**
 * Decodes a JSON resource file into an object of type T using a default deserialization strategy.
 *
 * This function is a convenience wrapper around the `decodeFromResource` function that uses a default deserialization
 * strategy.
 * The default deserialization strategy is obtained by calling `serializersModule.serializer<T>()`.
 *
 * @param path The path to the JSON resource file. The path should be relative to the `resources` directory.
 *
 * @return The object of type T obtained by decoding the JSON resource file.
 *
 * @throws NoSuchElementException If the resource file at the specified path does not exist.
 */
inline fun <reified T> Json.decodeFromResource(path: String): T =
    decodeFromResource(serializersModule.serializer<T>(), path)
