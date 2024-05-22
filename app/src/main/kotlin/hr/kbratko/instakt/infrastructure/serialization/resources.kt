package hr.kbratko.instakt.infrastructure.serialization

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.hocon.Hocon
import kotlinx.serialization.json.Json
import kotlinx.serialization.properties.Properties

/**
 * A singleton object that provides methods for decoding resource files.
 *
 * This object provides two methods for decoding resource files:
 * - `properties`: Decodes a resource file into an object of type T using the `Properties.decodeFromResource` function.
 * - `json`: Decodes a resource file into an object of type T using the `Json.decodeFromResource` function.
 *
 * This object also provides two nested objects for accessing instances of `LazyResources` and `AsyncResources`:
 * - `lazy`: An instance of `LazyResources`.
 * - `async`: An instance of `AsyncResources`.
 */
object Resources {
    /**
     * An instance of `LazyResources`.
     * Provides two methods for decoding resource files:
     * - `properties`: Decodes a resource file into an object of type T using the `Properties.decodeFromResource`
     * function.
     * - `json`: Decodes a resource file into an object of type T using the `Json.decodeFromResource` function.
     * Both methods are computed lazily.
     */
    val lazy = object {
        /**
         * Decodes a resource file into an object of type T using the `Properties.decodeFromResource` function.
         * The result is computed lazily.
         *
         * @param path The path to the resource file. The path should be relative to the `resources` directory.
         *
         * @return The object of type T obtained by decoding the resource file.
         *
         * @throws NoSuchElementException If the resource file at the specified path does not exist.
         */
        @OptIn(ExperimentalSerializationApi::class)
        inline fun <reified T> properties(path: String) = lazy { Properties.decodeFromResource<T>(path) }

        /**
         * Decodes a resource file into an object of type T using the `Properties.decodeFromResource` function.
         * The result is computed lazily. This function is specifically designed to handle HOCON
         * (Human-Optimized Config Object Notation) files.
         *
         * @param path The path to the resource file. The path should be relative to the `resources` directory.
         *
         * @return The object of type T obtained by decoding the resource file.
         *
         * @throws NoSuchElementException If the resource file at the specified path does not exist.
         */
        @OptIn(ExperimentalSerializationApi::class)
        inline fun <reified T> hocon(path: String) = lazy { Hocon.decodeFromResource<T>(path) }

        /**
         * Decodes a resource file into an object of type T using the `Json.decodeFromResource` function.
         * The result is computed lazily.
         *
         * @param path The path to the resource file. The path should be relative to the `resources` directory.
         *
         * @return The object of type T obtained by decoding the resource file.
         *
         * @throws NoSuchElementException If the resource file at the specified path does not exist.
         */
        inline fun <reified T> json(path: String) = lazy { Json.decodeFromResource<T>(path) }
    }

    /**
     * An instance of `AsyncResources`.
     * Provides two methods for decoding resource files:
     * - `properties`: Decodes a resource file into an object of type T using the `Properties.decodeFromResource`
     * function.
     * - `json`: Decodes a resource file into an object of type T using the `Json.decodeFromResource` function.
     * Both methods are computed asynchronously.
     */
    val suspended = object {
        /**
         * Decodes a resource file into an object of type T using the `Properties.decodeFromResource` function.
         * The result is computed asynchronously.
         *
         * @param path The path to the resource file. The path should be relative to the `resources` directory.
         * @param dispatcher The CoroutineDispatcher to be used for the computation.
         *
         * @return The object of type T obtained by decoding the resource file.
         *
         * @throws NoSuchElementException If the resource file at the specified path does not exist.
         */
        @OptIn(ExperimentalSerializationApi::class)
        suspend inline fun <reified T> properties(path: String, dispatcher: CoroutineDispatcher = Dispatchers.IO) =
            withContext(dispatcher) {
                Properties.decodeFromResource<T>(path)
            }

        /**
         * Decodes a resource file into an object of type T using the `Hocon.decodeFromResource` function.
         * The result is computed asynchronously. This function is specifically designed to handle HOCON
         * (Human-Optimized Config Object Notation) files.
         *
         * @param path The path to the resource file. The path should be relative to the `resources` directory.
         * @param dispatcher The CoroutineDispatcher to be used for the computation. Defaults to Dispatchers.IO.
         *
         * @return The object of type T obtained by decoding the resource file.
         *
         * @throws NoSuchElementException If the resource file at the specified path does not exist.
         */
        @OptIn(ExperimentalSerializationApi::class)
        suspend inline fun <reified T> hocon(path: String, dispatcher: CoroutineDispatcher = Dispatchers.IO) =
            withContext(dispatcher) {
                Hocon.decodeFromResource<T>(path)
            }

        /**
         * Decodes a resource file into an object of type T using the `Json.decodeFromResource` function.
         * The result is computed asynchronously.
         *
         * @param path The path to the resource file. The path should be relative to the `resources` directory.
         * @param dispatcher The CoroutineDispatcher to be used for the computation.
         *
         * @return The object of type T obtained by decoding the resource file.
         *
         * @throws NoSuchElementException If the resource file at the specified path does not exist.
         */
        suspend inline fun <reified T> json(path: String, dispatcher: CoroutineDispatcher = Dispatchers.IO) =
            withContext(dispatcher) {
                Json.decodeFromResource<T>(path)
            }
    }

    /**
     * Decodes a resource file into an object of type T using the `Properties.decodeFromResource` function.
     *
     * @param path The path to the resource file. The path should be relative to the `resources` directory.
     *
     * @return The object of type T obtained by decoding the resource file.
     *
     * @throws NoSuchElementException If the resource file at the specified path does not exist.
     */
    @OptIn(ExperimentalSerializationApi::class)
    inline fun <reified T> properties(path: String) = Properties.decodeFromResource<T>(path)

    /**
     * Decodes a resource file into an object of type T using the `Hocon.decodeFromResource` function.
     *
     * @param path The path to the resource file. The path should be relative to the `resources` directory.
     *
     * @return The object of type T obtained by decoding the resource file.
     *
     * @throws NoSuchElementException If the resource file at the specified path does not exist.
     */
    @OptIn(ExperimentalSerializationApi::class)
    inline fun <reified T> hocon(path: String) = Hocon.decodeFromResource<T>(path)

    /**
     * Decodes a resource file into an object of type T using the `Json.decodeFromResource` function.
     *
     * @param path The path to the resource file. The path should be relative to the `resources` directory.
     *
     * @return The object of type T obtained by decoding the resource file.
     *
     * @throws NoSuchElementException If the resource file at the specified path does not exist.
     */
    inline fun <reified T> json(path: String) = Json.decodeFromResource<T>(path)
}
