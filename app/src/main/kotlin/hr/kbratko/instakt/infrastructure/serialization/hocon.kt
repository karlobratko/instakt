package hr.kbratko.instakt.infrastructure.serialization

import com.typesafe.config.ConfigFactory
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.hocon.Hocon
import kotlinx.serialization.serializer

/**
 * Decodes a resource file into an object of type T using the `Hocon.decodeFromConfig` function.
 * This function is specifically designed to handle HOCON (Human-Optimized Config Object Notation) files.
 *
 * @param deserializer The deserialization strategy to be used for decoding the resource file.
 * @param path The path to the resource file. The path should be relative to the `resources` directory.
 *
 * @return The object of type T obtained by decoding the resource file.
 */
@OptIn(ExperimentalSerializationApi::class)
fun <T> Hocon.decodeFromResource(deserializer: DeserializationStrategy<T>, path: String): T =
    decodeFromConfig(deserializer, ConfigFactory.parseResources(path))

/**
 * Decodes a resource file into an object of type T using the `Hocon.decodeFromResource` function.
 * This function uses the `serializer` function from the `serializersModule` to determine the deserialization strategy.
 *
 * @param path The path to the resource file. The path should be relative to the `resources` directory.
 *
 * @return The object of type T obtained by decoding the resource file.
 */
@OptIn(ExperimentalSerializationApi::class)
inline fun <reified T> Hocon.decodeFromResource(path: String): T =
    decodeFromResource(serializersModule.serializer<T>(), path)
