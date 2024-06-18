package hr.kbratko.instakt.domain.utility

import java.time.LocalTime
import java.time.OffsetDateTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.job
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toKotlinInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.math.pow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineStart.UNDISPATCHED as Undispatched
import kotlin.Int.Companion.MAX_VALUE as Max
import kotlin.contracts.InvocationKind.EXACTLY_ONCE as ExactlyOnce
import kotlin.time.Duration.Companion.INFINITE as Infinite
import kotlin.time.Duration.Companion.ZERO as Zero

typealias Broker<T> = MutableSharedFlow<T>

typealias Emitter<T> = FlowCollector<T>

typealias Collector<T> = SharedFlow<T>

fun <T> Broker() = MutableSharedFlow<T>()

fun String.toDuration() = Duration.parse(this)

fun OffsetDateTime.toKotlinInstant() = toInstant().toKotlinInstant()

fun Instant.toLocalDate(timeZone: TimeZone) = toLocalDateTime(timeZone).date

fun java.time.LocalDateTime.withTimeAtStartOfDay(): java.time.LocalDateTime {
    return this.with(LocalTime.MIN)
}

@Serializable data class InstantClosedRange(
    override val start: Instant,
    override val endInclusive: Instant = Clock.System.now()
) : ClosedRange<Instant>

/**
 * This function is an extension function for the String class. It converts the first character of the string
 * to uppercase.
 * If the string is empty, it returns an empty string.
 *
 * @return A new string with the first character converted to uppercase. If the original string is empty, it returns
 * an empty string.
 */
fun String.uppercaseFirstChar() = this.lowercase().replaceFirstChar(Char::uppercaseChar)

/**
 * This function is a higher-order function that takes two arguments and a block of code, and executes the block with
 * the two arguments.
 * It is an inline function, which means the function will be inlined at the call site. This can lead to performance
 * improvements by avoiding function call overhead.
 * The function is annotated with `@OptIn(ExperimentalContracts::class)` to opt-in to the experimental
 * Kotlin Contracts API.
 * The function is also annotated with `@Suppress("SUBTYPING_BETWEEN_CONTEXT_RECEIVERS")` to suppress the
 * warning about subtyping between context receivers.
 *
 * @param A The type of the first argument.
 * @param B The type of the second argument.
 * @param R The return type of the block.
 * @param a The first argument.
 * @param b The second argument.
 * @param block The block of code to execute. This block is a lambda that takes two arguments of type A and B, and
 * returns a value of type R.
 * @return The result of the block execution.
 *
 * @throws Exception if the block throws an exception.
 */
@OptIn(ExperimentalContracts::class)
@Suppress("SUBTYPING_BETWEEN_CONTEXT_RECEIVERS")
inline fun <A, B, R> with(
    a: A,
    b: B,
    block: context(A, B) () -> R
): R {
    contract { callsInPlace(block, ExactlyOnce) }
    return block(a, b)
}

/**
 * This function is a higher-order function that takes three arguments and a block of code, and executes the block with
 * the three arguments.
 * It is an inline function, which means the function will be inlined at the call site. This can lead to performance
 * improvements by avoiding function call overhead.
 * The function is annotated with `@OptIn(ExperimentalContracts::class)` to opt-in to the experimental
 * Kotlin Contracts API.
 * The function is also annotated with `@Suppress("SUBTYPING_BETWEEN_CONTEXT_RECEIVERS")` to suppress the
 * warning about subtyping between context receivers.
 *
 * @param A The type of the first argument.
 * @param B The type of the second argument.
 * @param C The type of the third argument.
 * @param R The return type of the block.
 * @param a The first argument.
 * @param b The second argument.
 * @param c The third argument.
 * @param block The block of code to execute. This block is a lambda that takes three arguments of type A, B, and C, and
 * returns a value of type R.
 * @return The result of the block execution.
 *
 * @throws Exception if the block throws an exception.
 */
@OptIn(ExperimentalContracts::class)
@Suppress("SUBTYPING_BETWEEN_CONTEXT_RECEIVERS")
inline fun <A, B, C, R> with(
    a: A,
    b: B,
    c: C,
    block: context(A, B, C) () -> R
): R {
    contract { callsInPlace(block, ExactlyOnce) }
    return block(a, b, c)
}

/**
 * This function is a higher-order function that takes three arguments and a block of code, and executes the block with
 * the three arguments.
 * It is an inline function, which means the function will be inlined at the call site. This can lead to performance
 * improvements by avoiding function call overhead.
 * The function is annotated with `@OptIn(ExperimentalContracts::class)` to opt-in to the experimental
 * Kotlin Contracts API.
 * The function is also annotated with `@Suppress("SUBTYPING_BETWEEN_CONTEXT_RECEIVERS")` to suppress the
 * warning about subtyping between context receivers.
 *
 * @param A The type of the first argument.
 * @param B The type of the second argument.
 * @param C The type of the third argument.
 * @param D The type of the fourth argument.
 * @param R The return type of the block.
 * @param a The first argument.
 * @param b The second argument.
 * @param c The third argument.
 * @param d The fourth argument.
 * @param block The block of code to execute. This block is a lambda that takes three arguments of type A, B, and C, and
 * returns a value of type R.
 * @return The result of the block execution.
 *
 * @throws Exception if the block throws an exception.
 */
@OptIn(ExperimentalContracts::class)
@Suppress("SUBTYPING_BETWEEN_CONTEXT_RECEIVERS")
inline fun <A, B, C, D, R> with(
    a: A,
    b: B,
    c: C,
    d: D,
    block: context(A, B, C, D) () -> R
): R {
    contract { callsInPlace(block, ExactlyOnce) }
    return block(a, b, c, d)
}

/**
 * This function is a higher-order function that takes three arguments and a block of code, and executes the block with
 * the three arguments.
 * It is an inline function, which means the function will be inlined at the call site. This can lead to performance
 * improvements by avoiding function call overhead.
 * The function is annotated with `@OptIn(ExperimentalContracts::class)` to opt-in to the experimental
 * Kotlin Contracts API.
 * The function is also annotated with `@Suppress("SUBTYPING_BETWEEN_CONTEXT_RECEIVERS")` to suppress the
 * warning about subtyping between context receivers.
 *
 * @param A The type of the first argument.
 * @param B The type of the second argument.
 * @param C The type of the third argument.
 * @param D The type of the fourth argument.
 * @param E The type of the fifth argument.
 * @param R The return type of the block.
 * @param a The first argument.
 * @param b The second argument.
 * @param c The third argument.
 * @param d The fourth argument.
 * @param e The fifth argument.
 * @param block The block of code to execute. This block is a lambda that takes three arguments of type A, B, and C, and
 * returns a value of type R.
 * @return The result of the block execution.
 *
 * @throws Exception if the block throws an exception.
 */
@OptIn(ExperimentalContracts::class)
@Suppress("SUBTYPING_BETWEEN_CONTEXT_RECEIVERS")
inline fun <A, B, C, D, E, R> with(
    a: A,
    b: B,
    c: C,
    d: D,
    e: E,
    block: context(A, B, C, D, E) () -> R
): R {
    contract { callsInPlace(block, ExactlyOnce) }
    return block(a, b, c, d, e)
}

/**
 * This function is a higher-order function that takes three arguments and a block of code, and executes the block with
 * the three arguments.
 * It is an inline function, which means the function will be inlined at the call site. This can lead to performance
 * improvements by avoiding function call overhead.
 * The function is annotated with `@OptIn(ExperimentalContracts::class)` to opt-in to the experimental
 * Kotlin Contracts API.
 * The function is also annotated with `@Suppress("SUBTYPING_BETWEEN_CONTEXT_RECEIVERS")` to suppress the
 * warning about subtyping between context receivers.
 *
 * @param A The type of the first argument.
 * @param B The type of the second argument.
 * @param C The type of the third argument.
 * @param D The type of the fourth argument.
 * @param E The type of the fifth argument.
 * @param F The type of the sixth argument.
 * @param R The return type of the block.
 * @param a The first argument.
 * @param b The second argument.
 * @param c The third argument.
 * @param d The fourth argument.
 * @param e The fifth argument.
 * @param f The sixth argument.
 * @param block The block of code to execute. This block is a lambda that takes three arguments of type A, B, and C, and
 * returns a value of type R.
 * @return The result of the block execution.
 *
 * @throws Exception if the block throws an exception.
 */
@OptIn(ExperimentalContracts::class)
@Suppress("SUBTYPING_BETWEEN_CONTEXT_RECEIVERS")
inline fun <A, B, C, D, E, F, R> with(
    a: A,
    b: B,
    c: C,
    d: D,
    e: E,
    f: F,
    block: context(A, B, C, D, E, F) () -> R
): R {
    contract { callsInPlace(block, ExactlyOnce) }
    return block(a, b, c, d, e, f)
}

/**
 * This function is a higher-order function that takes three arguments and a block of code, and executes the block with
 * the three arguments.
 * It is an inline function, which means the function will be inlined at the call site. This can lead to performance
 * improvements by avoiding function call overhead.
 * The function is annotated with `@OptIn(ExperimentalContracts::class)` to opt-in to the experimental
 * Kotlin Contracts API.
 * The function is also annotated with `@Suppress("SUBTYPING_BETWEEN_CONTEXT_RECEIVERS")` to suppress the
 * warning about subtyping between context receivers.
 *
 * @param A The type of the first argument.
 * @param B The type of the second argument.
 * @param C The type of the third argument.
 * @param D The type of the fourth argument.
 * @param E The type of the fifth argument.
 * @param F The type of the sixth argument.
 * @param G The type of the seventh argument.
 * @param R The return type of the block.
 * @param a The first argument.
 * @param b The second argument.
 * @param c The third argument.
 * @param d The fourth argument.
 * @param e The fifth argument.
 * @param f The sixth argument.
 * @param g The seventh argument.
 * @param block The block of code to execute. This block is a lambda that takes three arguments of type A, B, and C, and
 * returns a value of type R.
 * @return The result of the block execution.
 *
 * @throws Exception if the block throws an exception.
 */
@OptIn(ExperimentalContracts::class)
@Suppress("SUBTYPING_BETWEEN_CONTEXT_RECEIVERS")
inline fun <A, B, C, D, E, F, G, R> with(
    a: A,
    b: B,
    c: C,
    d: D,
    e: E,
    f: F,
    g: G,
    block: context(A, B, C, D, E, F, G) () -> R
): R {
    contract { callsInPlace(block, ExactlyOnce) }
    return block(a, b, c, d, e, f, g)
}

/**
 * This function is a higher-order function that takes three arguments and a block of code, and executes the block with
 * the three arguments.
 * It is an inline function, which means the function will be inlined at the call site. This can lead to performance
 * improvements by avoiding function call overhead.
 * The function is annotated with `@OptIn(ExperimentalContracts::class)` to opt-in to the experimental
 * Kotlin Contracts API.
 * The function is also annotated with `@Suppress("SUBTYPING_BETWEEN_CONTEXT_RECEIVERS")` to suppress the
 * warning about subtyping between context receivers.
 *
 * @param A The type of the first argument.
 * @param B The type of the second argument.
 * @param C The type of the third argument.
 * @param D The type of the fourth argument.
 * @param E The type of the fifth argument.
 * @param F The type of the sixth argument.
 * @param G The type of the seventh argument.
 * @param H The type of the eight argument.
 * @param R The return type of the block.
 * @param a The first argument.
 * @param b The second argument.
 * @param c The third argument.
 * @param d The fourth argument.
 * @param e The fifth argument.
 * @param f The sixth argument.
 * @param g The seventh argument.
 * @param h The eight argument.
 * @param block The block of code to execute. This block is a lambda that takes three arguments of type A, B, and C, and
 * returns a value of type R.
 * @return The result of the block execution.
 *
 * @throws Exception if the block throws an exception.
 */
@OptIn(ExperimentalContracts::class)
@Suppress("SUBTYPING_BETWEEN_CONTEXT_RECEIVERS")
inline fun <A, B, C, D, E, F, G, H, R> with(
    a: A,
    b: B,
    c: C,
    d: D,
    e: E,
    f: F,
    g: G,
    h: H,
    block: context(A, B, C, D, E, F, G, H) () -> R
): R {
    contract { callsInPlace(block, ExactlyOnce) }
    return block(a, b, c, d, e, f, g, h)
}

/**
 * This function is a higher-order function that takes three arguments and a block of code, and executes the block with
 * the three arguments.
 * It is an inline function, which means the function will be inlined at the call site. This can lead to performance
 * improvements by avoiding function call overhead.
 * The function is annotated with `@OptIn(ExperimentalContracts::class)` to opt-in to the experimental
 * Kotlin Contracts API.
 * The function is also annotated with `@Suppress("SUBTYPING_BETWEEN_CONTEXT_RECEIVERS")` to suppress the
 * warning about subtyping between context receivers.
 *
 * @param A The type of the first argument.
 * @param B The type of the second argument.
 * @param C The type of the third argument.
 * @param D The type of the fourth argument.
 * @param E The type of the fifth argument.
 * @param F The type of the sixth argument.
 * @param G The type of the seventh argument.
 * @param H The type of the eight argument.
 * @param I The type of the ninth argument.
 * @param R The return type of the block.
 * @param a The first argument.
 * @param b The second argument.
 * @param c The third argument.
 * @param d The fourth argument.
 * @param e The fifth argument.
 * @param f The sixth argument.
 * @param g The seventh argument.
 * @param h The eight argument.
 * @param i The ninth argument.
 * @param block The block of code to execute. This block is a lambda that takes three arguments of type A, B, and C, and
 * returns a value of type R.
 * @return The result of the block execution.
 *
 * @throws Exception if the block throws an exception.
 */
@OptIn(ExperimentalContracts::class)
@Suppress("SUBTYPING_BETWEEN_CONTEXT_RECEIVERS")
inline fun <A, B, C, D, E, F, G, H, I, R> with(
    a: A,
    b: B,
    c: C,
    d: D,
    e: E,
    f: F,
    g: G,
    h: H,
    i: I,
    block: context(A, B, C, D, E, F, G, H, I) () -> R
): R {
    contract { callsInPlace(block, ExactlyOnce) }
    return block(a, b, c, d, e, f, g, h, i)
}

/**
 * This function is a concurrency-limited version of the standard `map` function.
 * It transforms a list of elements of type `A` into a list of elements of type `B` using a provided
 * transformation function.
 * The transformation is performed asynchronously, with a limit on the number of concurrent transformations.
 *
 * @param concurrencyLimit The maximum number of concurrent transformations. Defaults to `Int.MAX_VALUE` if
 * not provided.
 * @param transformation The function to use to transform elements of type `A` into elements of type `B`.
 * @return A list of transformed elements of type `B`.
 *
 * @throws Exception if the transformation function throws an exception.
 */
suspend fun <A, B> List<A>.mapAsync(
    concurrencyLimit: Int = Max,
    transformation: suspend (A) -> B
): List<B> =
    coroutineScope {
        // Create a semaphore with a number of permits equal to the concurrency limit
        val semaphore = Semaphore(concurrencyLimit)

        // For each element in the list, launch an asynchronous coroutine to transform the element
        // The semaphore is used to limit the number of concurrent transformations
        this@mapAsync
            .map {
                async {
                    semaphore.withPermit {
                        transformation(it)
                    }
                }
            }
            // Wait for all transformations to complete and collect the results into a list
            .awaitAll()
    }

private val Unset = Any()

/**
 * This function provides a lazy initialization for suspend functions.
 * It takes an initializer suspend function as an argument and returns a new suspend function.
 * The returned function, when invoked, will call the initializer function only once and cache its result.
 * All subsequent invocations of the returned function will return the cached result.
 * The function is thread-safe, meaning it ensures that the initializer function is called only once even in a
 * multithreaded environment.
 *
 * @param initializer The suspend function to be lazily initialized. This function is invoked the first time the
 * returned function is called.
 * @return A new suspend function that, when invoked, will call the initializer function only once and cache its result.
 *         All subsequent invocations of the returned function will return the cached result.
 *
 * @throws ClassCastException if the initializer function returns a value that cannot be cast to the type `A`.
 */
@Suppress("UNCHECKED_CAST", "NAME_SHADOWING")
fun <A> suspendLazy(initializer: suspend () -> A): suspend () -> A {
    var initializer: (suspend () -> A)? = initializer
    val mutex = Mutex()
    var holder: Any? = Unset

    return {
        if (initializer == null) {
            holder as A
        } else {
            mutex.withLock {
                initializer?.let {
                    holder = it()
                    initializer = null
                }
                holder as A
            }
        }
    }
}

/**
 * A class representing a connection pool for a specific type of connections.
 * Each connection is represented as a Flow of values of type `V`.
 * The connections are created by a provided builder function.
 * The class is thread-safe and ensures that each connection is created only once and then shared among all users.
 *
 * @param scope The CoroutineScope in which the connections are shared.
 * @param stopTimeout The duration after which a connection is closed when it's not subscribed to. Defaults to
 * `Duration.ZERO` if not provided.
 * @param replay The number of values to replay to new subscribers. Defaults to `0` if not provided.
 * @param replayExpiration The duration after which old values are no longer replayed. Defaults to `Duration.INFINITE`
 * if not provided.
 * @param builder The function to use to build new connections.
 */
class ConnectionPool<K, V>(
    private val scope: CoroutineScope,
    private val stopTimeout: Duration = Zero,
    private val replay: Int = 0,
    private val replayExpiration: Duration = Infinite,
    private val builder: (K) -> Flow<V>
) {
    // A map to store the connections. Each connection is associated with a key of type `K`.
    private val connections = mutableMapOf<K, Flow<V>>()

    /**
     * Gets the connection associated with the provided key.
     * If the connection does not exist, it is created using the builder function and then stored in the map.
     * The connection is shared in the provided CoroutineScope and replay parameters are applied.
     *
     * @param key The key associated with the connection.
     * @return The Flow representing the connection.
     */
    operator fun get(key: K): Flow<V> =
        synchronized(this) {
            connections.getOrPut(key) {
                builder(key).shareIn(
                    scope = scope,
                    started = WhileSubscribed(
                        stopTimeoutMillis = stopTimeout.inWholeMilliseconds,
                        replayExpirationMillis = replayExpiration.inWholeMilliseconds
                    ),
                    replay = replay
                )
            }
        }
}

/**
 * This function is used to race multiple suspend functions against each other.
 * It takes a variable number of suspend functions as arguments and returns the result of the function that
 * completes first.
 * The function is implemented using coroutines and the `select` expression from the kotlinx.coroutines library.
 * The `select` expression allows to race multiple coroutines and select the first one that completes.
 * All other coroutines are cancelled after the first one completes.
 *
 * @param fns The suspend functions to race against each other. These functions are invoked in the context of a
 * new coroutine.
 * @return The result of the suspend function that completes first.
 *
 * @throws CancellationException if the parent coroutine is cancelled.
 */
suspend fun <A> raceOf(vararg fns: suspend CoroutineScope.() -> A) =
    coroutineScope {
        with(Job(parent = coroutineContext.job)) job@{
            select {
                fns.asSequence()
                    .map {
                        async(
                            context = this@job + CoroutineName("racer"),
                            start = Undispatched,
                            block = it
                        )
                    }
                    .forEach { deferred -> deferred.onAwait { it } }
            }.also { this@job.cancel() }
        }
    }

/**
 * This function is used to retry a suspend function with a backoff strategy when an exception occurs.
 * It takes an operation suspend function as an argument and returns the result of the function when it completes
 * without throwing an exception.
 * If the operation function throws an exception, the function checks if the exception satisfies a provided predicate.
 * If the predicate returns `true`, the function waits for a delay and then retries the operation.
 * The delay is calculated as a power of 2 of the number of attempts, multiplied by a provided base duration.
 * The function continues to retry the operation until it completes without throwing an exception or the predicate
 * returns `false`.
 *
 * @param backoffBase The base duration for the backoff strategy. Defaults to `100.milliseconds` if not provided.
 * @param predicate The predicate to use to decide whether to retry the operation when an exception occurs. The
 * predicate takes the exception and the number of attempts as arguments.
 * @param operation The suspend function to retry. This function is invoked in the context of a new coroutine.
 * @return The result of the operation function when it completes without throwing an exception.
 *
 * @throws Throwable if the operation function throws an exception that does not satisfy the predicate.
 * @throws CancellationException if the parent coroutine is cancelled.
 */
suspend inline fun <A> retryWithBackoffWhen(
    backoffBase: Duration = 100.milliseconds,
    predicate: (Throwable, Int) -> Boolean,
    operation: () -> A
): A {
    var attempt = 0
    var fromDownstream: Throwable? = null

    while (true) {
        try {
            return operation()
        } catch (e: Throwable) {
            if (fromDownstream != null) {
                e.addSuppressed(fromDownstream)
            }

            fromDownstream = e
            if (e is CancellationException || !predicate(e, attempt++)) {
                throw e
            }
        }
        val times = 2.0.pow(attempt).toInt()
        delay(backoffBase * times)
    }
}

/**
 * This function is used to retry a suspend function with a backoff strategy when an exception occurs.
 * It takes an operation suspend function as an argument and returns the result of the function when it completes
 * without throwing an exception.
 * If the operation function throws an exception, the function checks if the exception satisfies a provided predicate.
 * If the predicate returns `true`, the function waits for a delay and then retries the operation.
 * The delay is calculated as a power of 2 of the number of attempts, multiplied by a provided base duration.
 * The function continues to retry the operation until it completes without throwing an exception or the predicate
 * returns `false`.
 * This function is a simplified version of `retryWithBackoffWhen` where the number of retries and the predicate are
 * provided as separate arguments.
 *
 * @param retries The maximum number of retries. Defaults to `Int.MAX_VALUE` if not provided.
 * @param backoffBase The base duration for the backoff strategy. Defaults to `100.milliseconds` if not provided.
 * @param predicate The predicate to use to decide whether to retry the operation when an exception occurs. The
 * predicate takes the exception as an argument. Defaults to a function that always returns `true` if not provided.
 * @param operation The suspend function to retry. This function is invoked in the context of a new coroutine.
 * @return The result of the operation function when it completes without throwing an exception.
 *
 * @throws Throwable if the operation function throws an exception that does not satisfy the predicate.
 * @throws CancellationException if the parent coroutine is cancelled.
 */
suspend inline fun <A> retryWithBackoff(
    retries: Int = Max,
    backoffBase: Duration = 100.milliseconds,
    predicate: (Throwable) -> Boolean = { true },
    operation: () -> A
): A =
    retryWithBackoffWhen(
        backoffBase = backoffBase,
        predicate = { cause, attempt -> attempt < retries && predicate(cause) },
        operation = operation
    )

/**
 * This function is used to retry an operation when an exception occurs.
 * It takes an operation function and a predicate as arguments.
 * The operation function is invoked and its result is returned if it completes without throwing an exception.
 * If the operation function throws an exception, the function checks if the exception satisfies the provided predicate.
 * The predicate takes the exception and the number of attempts as arguments.
 * If the predicate returns `true`, the operation is retried.
 * The function continues to retry the operation until it completes without throwing an exception or the predicate
 * returns `false`.
 *
 * @param predicate The predicate to use to decide whether to retry the operation when an exception occurs. The
 * predicate takes the exception and the number of attempts as arguments.
 * @param operation The function to retry. This function is invoked and its result is returned if it completes without
 * throwing an exception.
 * @return The result of the operation function when it completes without throwing an exception.
 *
 * @throws Throwable if the operation function throws an exception that does not satisfy the predicate.
 * @throws CancellationException if the operation function throws a CancellationException.
 */
inline fun <A> retryWhen(
    predicate: (Throwable, Int) -> Boolean,
    operation: () -> A
): A {
    var attempt = 0
    var fromDownstream: Throwable? = null

    while (true) {
        try {
            return operation()
        } catch (e: Throwable) {
            if (fromDownstream != null) {
                e.addSuppressed(fromDownstream)
            }

            fromDownstream = e
            if (e is CancellationException || !predicate(e, attempt++)) {
                throw e
            }
        }
    }
}

/**
 * This function is used to retry an operation when an exception occurs.
 * It takes an operation function and a predicate as arguments.
 * The operation function is invoked and its result is returned if it completes without throwing an exception.
 * If the operation function throws an exception, the function checks if the exception satisfies the provided predicate.
 * The predicate takes the exception as an argument.
 * If the predicate returns `true`, the operation is retried.
 * The function continues to retry the operation until it completes without throwing an exception or the predicate
 * returns `false`.
 * This function is a simplified version of `retryWhen` where the number of retries and the predicate are provided as
 * separate arguments.
 *
 * @param retries The maximum number of retries. Defaults to `Int.MAX_VALUE` if not provided.
 * @param predicate The predicate to use to decide whether to retry the operation when an exception occurs. The
 * predicate takes the exception as an argument. Defaults to a function that always returns `true` if not provided.
 * @param operation The function to retry. This function is invoked and its result is returned if it completes without
 * throwing an exception.
 * @return The result of the operation function when it completes without throwing an exception.
 *
 * @throws Throwable if the operation function throws an exception that does not satisfy the predicate.
 */
inline fun <A> retry(
    retries: Int = Max,
    predicate: (Throwable) -> Boolean = { true },
    operation: () -> A
): A =
    retryWhen(
        predicate = { cause, attempt -> attempt < retries && predicate(cause) },
        operation = operation
    )
