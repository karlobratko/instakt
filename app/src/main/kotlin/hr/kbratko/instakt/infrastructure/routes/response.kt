package hr.kbratko.instakt.infrastructure.routes

import arrow.core.Either
import arrow.core.EitherNel
import arrow.core.Nel
import arrow.core.toNonEmptyListOrNull
import hr.kbratko.instakt.domain.DomainError
import hr.kbratko.instakt.domain.config.DefaultInstantProvider
import hr.kbratko.instakt.domain.conversion.ConversionScope
import hr.kbratko.instakt.infrastructure.errors.NelErrorToHttpStatusCodeConversion
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.EncodeDefault.Mode.NEVER as Never

/**
 * A sealed class representing a response with a timestamp.
 * This class is serializable with a custom serializer, ResponseSerializer.
 *
 * @param A The type of the data in a successful response.
 */
@Serializable(with = ResponseSerializer::class)
sealed class Response<out A> {
    /**
     * The timestamp of the response, represented as the number of milliseconds since the Unix epoch.
     */
    val timestamp = DefaultInstantProvider.now().toEpochMilliseconds()

    abstract val code: HttpStatusCode

    /**
     * A data class representing a successful response.
     * This class is a subtype of Response.
     *
     * @param A The type of the data in the response.
     * @property data The data in the response.
     */
    data class Success<out A> private constructor(val data: A, override val code: HttpStatusCode) : Response<A>() {
        companion object {
            /**
             * A function to create a new Success object cast as Response<A> for serialization purposes.
             *
             * @param data The data in the response.
             * @return A new Success object with the given data.
             */
            operator fun <A> invoke(data: A, code: HttpStatusCode): Response<A> = Success(data, code)
        }
    }

    /**
     * A data class representing a failed response.
     * This class is a subtype of Response and contains a non-empty list of error messages.
     *
     * @property errors The non-empty list of error messages.
     */
    data class Failure private constructor(
        val errors: Nel<String>,
        override val code: HttpStatusCode
    ) : Response<Nothing>() {
        companion object {
            /**
             * A function to create a new Failure object cast as Response<Nothing> for serialization purposes.
             *
             * @param errors The non-empty list of error messages.
             * @return A new Failure object with the given error messages.
             */
            operator fun invoke(errors: Nel<String>, code: HttpStatusCode): Response<Nothing> = Failure(errors, code)
        }
    }
}

/**
 * Extension function on NonEmptyList of DomainError to convert it into a Failure Response.
 * This function uses the NelErrorToHttpStatusCodeConversion to convert the DomainError into an HttpStatusCode.
 *
 * @return A Failure Response object.
 */
fun Nel<DomainError>.toFailure() = toFailure(NelErrorToHttpStatusCodeConversion)

/**
 * Extension function on NonEmptyList of Error to convert it into a Failure Response.
 * This function uses a provided HttpStatusCode to create a Failure Response.
 *
 * @param statusCode The HttpStatusCode to use for the Failure Response.
 * @return A Failure Response object.
 */
fun <Error> Nel<Error>.toFailure(statusCode: HttpStatusCode) = toFailure { statusCode }

/**
 * Extension function on NonEmptyList of Error to convert it into a Failure Response.
 * This function uses a provided ConversionScope to convert the Error into an HttpStatusCode.
 *
 * @param onErrorConversionScope The ConversionScope to use to convert the Error into an HttpStatusCode.
 * @return A Failure Response object.
 */
fun <Error> Nel<Error>.toFailure(onErrorConversionScope: ConversionScope<Nel<Error>, HttpStatusCode>) =
    toFailure { with(onErrorConversionScope) { it.convert() } }

/**
 * Extension function on NonEmptyList of Error to convert it into a Failure Response.
 * This function uses a provided function to convert the Error into an HttpStatusCode.
 *
 * @param onErrorMapper The function to use to convert the Error into an HttpStatusCode.
 * @return A Failure Response object.
 */
fun <Error> Nel<Error>.toFailure(onErrorMapper: (Nel<Error>) -> HttpStatusCode) =
    Response.Failure(map { it.toString() }, onErrorMapper.invoke(this))

/**
 * Extension function on EitherNel<DomainError, A> to convert it into a Response.
 * This function uses the NelErrorToHttpStatusCodeConversion to convert the DomainError into an HttpStatusCode.
 *
 * @param onSuccessStatusCode The HttpStatusCode to use if the Either is a Right.
 * @return A Response object.
 */
fun <A> EitherNel<DomainError, A>.toResponse(
    onSuccessStatusCode: HttpStatusCode
) = toResponse(NelErrorToHttpStatusCodeConversion, onSuccessStatusCode)

/**
 * Extension function on EitherNel<Error, A> to convert it into a Response.
 * This function uses a provided ConversionScope to convert the Error into an HttpStatusCode.
 *
 * @param onErrorConversionScope The ConversionScope to use to convert the Error into an HttpStatusCode.
 * @param onSuccessStatusCode The HttpStatusCode to use if the Either is a Right.
 * @return A Response object.
 */
fun <Error, A> EitherNel<Error, A>.toResponse(
    onErrorConversionScope: ConversionScope<Nel<Error>, HttpStatusCode>,
    onSuccessStatusCode: HttpStatusCode
) = toResponse({ with(onErrorConversionScope) { it.convert() } }, onSuccessStatusCode)

/**
 * Extension function on EitherNel<Error, A> to convert it into a Response.
 * This function uses a provided function to convert the Error into an HttpStatusCode.
 *
 * @param onErrorMapper The function to use to convert the Error into an HttpStatusCode.
 * @param onSuccessStatusCode The HttpStatusCode to use if the Either is a Right.
 * @return A Response object.
 */
fun <Error, A> EitherNel<Error, A>.toResponse(
    onErrorMapper: (Nel<Error>) -> HttpStatusCode,
    onSuccessStatusCode: HttpStatusCode
) = when (this) {
    is Either.Left -> value.toFailure(onErrorMapper)
    is Either.Right -> Response.Success(value, onSuccessStatusCode)
}

class ResponseSerializer<A>(tSerializer: KSerializer<A>) : KSerializer<Response<A>> {

    @Serializable
    @SerialName("Response")
    @OptIn(ExperimentalSerializationApi::class)
    data class ResponseSurrogate<A>(
        val status: Status,
        val code: Int,
        val timestamp: Long,
        @EncodeDefault(mode = Never) val data: A? = null,
        @EncodeDefault(mode = Never) val errors: List<String>? = null
    ) {
        enum class Status {
            @SerialName("success")
            Success,

            @SerialName("failure")
            Failure
        }
    }

    private val surrogateSerializer = ResponseSurrogate.serializer(tSerializer)

    override val descriptor: SerialDescriptor = surrogateSerializer.descriptor

    override fun deserialize(decoder: Decoder): Response<A> {
        val surrogate = surrogateSerializer.deserialize(decoder)
        return when (surrogate.status) {
            ResponseSurrogate.Status.Success ->
                if (surrogate.data != null) {
                    Response.Success(surrogate.data, HttpStatusCode.fromValue(surrogate.code))
                } else {
                    throw SerializationException("Missing data for successful result.")
                }

            ResponseSurrogate.Status.Failure ->
                if (!surrogate.errors.isNullOrEmpty()) {
                    Response.Failure(
                        surrogate.errors.toNonEmptyListOrNull()!!,
                        HttpStatusCode.fromValue(surrogate.code)
                    )
                } else {
                    throw SerializationException("Missing errors for failing result.")
                }
        }
    }

    override fun serialize(encoder: Encoder, value: Response<A>) {
        val surrogate = when (value) {
            is Response.Success -> ResponseSurrogate(
                ResponseSurrogate.Status.Success,
                code = value.code.value,
                timestamp = value.timestamp,
                data = value.data
            )

            is Response.Failure -> ResponseSurrogate(
                ResponseSurrogate.Status.Failure,
                code = value.code.value,
                timestamp = value.timestamp,
                errors = value.errors
            )
        }
        surrogateSerializer.serialize(encoder, surrogate)
    }
}
