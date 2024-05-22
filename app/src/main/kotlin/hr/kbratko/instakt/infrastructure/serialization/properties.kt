package hr.kbratko.instakt.infrastructure.serialization

import java.io.InputStream
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.properties.Properties
import kotlinx.serialization.serializer

private fun java.util.Properties.toStringMap(): Map<String, String> =
    buildMap {
        for (name in stringPropertyNames())
            put(name, getProperty(name))
    }

/**
 * Decodes a stream into an object of type T using a provided deserialization strategy.
 *
 * This function is used to read a stream and convert it into an object of type T.
 * The conversion is done using the provided deserialization strategy.
 * The stream is loaded into a Properties object, which is then converted into a String map.
 * The String map is then decoded into an object of type T.
 *
 * @param deserializer The deserialization strategy to be used for converting the stream into an object of type T.
 * @param stream The InputStream to be decoded.
 *
 * @return The object of type T obtained by decoding the stream.
 */
@OptIn(ExperimentalSerializationApi::class)
fun <T> Properties.decodeFromStream(deserializer: DeserializationStrategy<T>, stream: InputStream): T =
    decodeFromStringMap(
        deserializer,
        java.util.Properties()
            .apply { load(stream) }
            .toStringMap()
    )

/**
 * Decodes a stream into an object of type T using a default deserialization strategy.
 *
 * This function is a convenience wrapper around the `decodeFromStream` function that uses a default deserialization
 * strategy.
 * The default deserialization strategy is obtained by calling `serializersModule.serializer<T>()`.
 *
 * @param stream The InputStream to be decoded.
 *
 * @return The object of type T obtained by decoding the stream.
 */
@OptIn(ExperimentalSerializationApi::class)
inline fun <reified T> Properties.decodeFromStream(stream: InputStream): T =
    decodeFromStream(serializersModule.serializer<T>(), stream)

/**
 * Decodes a resource file into an object of type T using a provided deserialization strategy.
 *
 * This function is used to read a resource file and convert it into an object of type T.
 * The conversion is done using the provided deserialization strategy.
 * The resource file is loaded into an InputStream, which is then passed to the `decodeFromStream` function.
 *
 * @param deserializer The deserialization strategy to be used for converting the resource file into an object of
 * type T.
 * @param path The path to the resource file. The path should be relative to the `resources` directory.
 *
 * @return The object of type T obtained by decoding the resource file.
 *
 * @throws NoSuchElementException If the resource file at the specified path does not exist.
 */
@OptIn(ExperimentalSerializationApi::class)
fun <T> Properties.decodeFromResource(deserializer: DeserializationStrategy<T>, path: String): T =
    decodeFromStream(
        deserializer,
        this::class.java.classLoader.getResourceAsStream(path)!!
    )

/**
 * Decodes a resource file into an object of type T using a default deserialization strategy.
 *
 * This function is a convenience wrapper around the `decodeFromResource` function that uses a default
 * deserialization strategy.
 * The default deserialization strategy is obtained by calling `serializersModule.serializer<T>()`.
 * The resource file is loaded into an InputStream, which is then passed to the `decodeFromResource` function.
 *
 * @param path The path to the resource file. The path should be relative to the `resources` directory.
 *
 * @return The object of type T obtained by decoding the resource file.
 *
 * @throws NoSuchElementException If the resource file at the specified path does not exist.
 */
@OptIn(ExperimentalSerializationApi::class)
inline fun <reified T> Properties.decodeFromResource(path: String): T =
    decodeFromResource(serializersModule.serializer<T>(), path)
