package hr.kbratko.instakt.domain.utility

import arrow.core.Either
import arrow.core.EitherNel
import arrow.core.Nel
import arrow.core.Option
import arrow.core.Option.Companion.catch
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.nel
import arrow.core.raise.Raise
import arrow.core.raise.RaiseAccumulate
import arrow.core.raise.either
import arrow.core.raise.fold
import arrow.core.right
import arrow.core.toNonEmptyListOrNone
import arrow.core.toOption
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.experimental.ExperimentalTypeInference
import kotlin.contracts.InvocationKind.AT_MOST_ONCE as AtMostOnce

fun OffsetDateTime?.toKotlinInstantOrNone() = toOption().map { it.toKotlinInstant() }

fun String.toUUIDOrNone(): Option<UUID> = catch { UUID.fromString(this) }

/**
 * This function is an inline function that takes a block of code and wraps it in an `EitherNel` type.
 * `EitherNel` is a type from the Arrow library that represents a value of one of two possible types (a disjoint union).
 * An instance of `EitherNel` is either an instance of `Left` or `Right`.
 * The function invokes the block of code within a `Raise` context. If the block of code raises an error, the function
 * will return a `Left` containing the error. If the block of code completes successfully, the function will return a
 * `Right` containing the result.
 *
 * @param Error The type of the error that can be raised.
 * @param A The type of the result of the block of code.
 * @param block The block of code to be invoked. This block is invoked within a `Raise` context.
 * @return An `EitherNel` containing either the error raised by the block of code (Either.Left), or the result of the
 * block of code (Either.Right).
 */
inline fun <Error, A> eitherNel(block: Raise<Nel<Error>>.() -> A): EitherNel<Error, A> =
    fold({ block.invoke(this) }, { Either.Left(it) }, { Either.Right(it) })

/**
 * This function is an extension function for the List class. It attempts to convert the list to a NonEmptyList (Nel).
 * If the conversion is successful (i.e., the list is not empty), it returns an Either.Right containing
 * the NonEmptyList.
 * If the conversion is not successful (i.e., the list is empty), it returns an Either.Left containing an error.
 *
 * @param Error The type of the error that can be raised.
 * @param A The type of the elements in the list.
 * @param error A function that creates an instance of `Error`. This function is called if the list is empty.
 * @return An Either containing the NonEmptyList if the conversion is successful (Either.Right), or an error if it is
 * not (Either.Left).
 */
fun <Error, A> List<A>.toNonEmptyListOrLeft(error: () -> Error): Either<Error, Nel<A>> =
    toNonEmptyListOrNone().toEither(error)

/**
 * This function is an extension function for the List class. It attempts to convert the list to a NonEmptyList (Nel).
 * If the conversion is successful (i.e., the list is not empty), it returns an Either.Right containing the
 * NonEmptyList wrapped in a Nel.
 * If the conversion is not successful (i.e., the list is empty), it returns an Either.Left containing an error wrapped
 * in a Nel.
 *
 * @param Error The type of the error that can be raised.
 * @param A The type of the elements in the list.
 * @param error A function that creates an instance of `Error`. This function is called if the list is empty.
 * @return An EitherNel containing the NonEmptyList if the conversion is successful (Either.Right), or an error if it
 * is not (Either.Left).
 */
fun <Error, A> List<A>.toNonEmptyListOrLeftNel(error: () -> Error): EitherNel<Error, Nel<A>> =
    toNonEmptyListOrLeft { error().nel() }

/**
 * Extension function for the String class to convert a string to a Long or None.
 *
 * This function attempts to convert the string on which it's invoked to a Long.
 * If the conversion is successful, it returns an Option containing the Long value.
 * If the conversion is not successful (for example, if the string does not represent a valid Long),
 * it returns None.
 *
 * @return An Option containing the Long value if the conversion is successful, or None if it is not.
 */
fun String.toLongOrNone(): Option<Long> = toLongOrNull().toOption()

/**
 * This function is an extension function for the String class. It attempts to convert the string to a Long.
 * If the conversion is successful, it returns an Either.Right containing the Long value.
 * If the conversion is not successful (for example, if the string does not represent a valid Long),
 * it returns an Either.Left containing an error.
 *
 * @param Error The type of the error that can be raised.
 * @param error A function that creates an instance of `Error`. This function is called if the string cannot be
 * converted to a Long.
 * @return An Either containing the Long value if the conversion is successful (Either.Right), or an error if it is
 * not (Either.Left).
 */
fun <Error> String.toLongOrLeft(error: () -> Error): Either<Error, Long> = toLongOrNone().toEither(error)

/**
 * This function is an extension function for the String class. It attempts to convert the string to a Long.
 * If the conversion is successful, it returns an Either.Right containing the Long value wrapped in a
 * NonEmptyList (Nel).
 * If the conversion is not successful (for example, if the string does not represent a valid Long),
 * it returns an Either.Left containing an error wrapped in a NonEmptyList.
 *
 * @param Error The type of the error that can be raised.
 * @param error A function that creates an instance of `Error`. This function is called if the string cannot be
 * converted to a Long.
 * @return An EitherNel containing the Long value if the conversion is successful (Either.Right), or an error if it is
 * not (Either.Left).
 */
fun <Error> String.toLongOrLeftNel(error: () -> Error): EitherNel<Error, Long> = toLongOrLeft { error().nel() }

/**
 * This function is an extension function on the `Option<A>` type from the Arrow library.
 * It is used to get the value from the `Option` if it is present (`Some`), or raise an error if it is not (`None`).
 * The error is created by the provided `error` function.
 *
 * @param Error The type of the error that can be raised.
 * @param A The type of the value contained in the `Option`.
 * @param error A function that creates an instance of `Error`. This function is called if the `Option` is `None`.
 * @return The value contained in the `Option` if it is `Some`.
 * @throws Error If the `Option` is `None`, the function raises an error of type `Error`.
 */
context(Raise<Error>)
inline fun <Error, A> Option<A>.getOrRaise(error: () -> Error) = getOrElse { raise(error()) }

/**
 * This function is an extension function on the `Option<A>` type from the Arrow library.
 * It is used to get the value from the `Option` if it is present (`Some`), or raise an error if it is not (`None`).
 * The error is created by the provided `error` function.
 *
 * @param Error The type of the error that can be raised.
 * @param A The type of the value contained in the `Option`.
 * @param error A function that creates an instance of `Error`. This function is called if the `Option` is `None`.
 * @return The value contained in the `Option` if it is `Some`.
 * @throws Error If the `Option` is `None`, the function raises an error of type `Error`.
 */
context(Raise<Nel<Error>>)
inline fun <Error, A> Option<A>.getOrRaiseNel(error: () -> Error) = getOrElse { raise(error().nel()) }

/**
 * This function is an extension function on the `Option<A>` type from the Arrow library.
 * It is used to convert the `Option` to an `EitherNel` type.
 * If the `Option` is `Some`, the contained value is wrapped in an `Either.Right`.
 * If the `Option` is `None`, an error is created by the provided `ifEmpty` function and wrapped in an `Either.Left`.
 *
 * @param Error The type of the error that can be raised.
 * @param A The type of the value contained in the `Option`.
 * @param ifEmpty A function that creates an instance of `Error`. This function is called if the `Option` is `None`.
 * @return An `EitherNel` containing either the error created by `ifEmpty` (Either.Left), or the value contained in
 * the `Option` (Either.Right).
 */
inline fun <Error, A> Option<A>.toEitherNel(ifEmpty: () -> Error): EitherNel<Error, A> = toEither { ifEmpty().nel() }

/**
 * This function is an extension function on the `Iterable<A>` type.
 * It is used to convert the iterable to a `NonEmptyList<A>` if it is not empty, or raise an error if it is empty.
 * The error is created by the provided `error` function.
 *
 * @param Error The type of the error that can be raised.
 * @param A The type of the elements in the iterable.
 * @param error A function that creates an instance of `Error`. This function is called if the iterable is empty.
 * @return A `NonEmptyList<A>` containing the elements of the iterable if it is not empty.
 * @throws Error If the iterable is empty, the function raises an error of type `Error`.
 */
context(Raise<Error>)
inline fun <Error, A> Iterable<A>.toNonEmptyListOrRaise(error: () -> Error): Nel<A> =
    toNonEmptyListOrNone().getOrRaise { error() }

/**
 * This function is used to wrap a block of code that can raise an error into an `EitherNel` type.
 * `EitherNel` is a type from the Arrow library that represents a value of one of two possible types (a disjoint union).
 * An instance of `EitherNel` is either an instance of `Left` or `Right`.
 * If the `block` raises an error, the function will return a `Left` containing the error. If the `block` completes
 * successfully, the function will return a `Right` containing the result.
 *
 * @param Error The type of the error that can be raised.
 * @param A The type of the result of the `block`.
 * @param block A block of code that can raise an error. This block is a lambda with receiver, meaning it can be called
 * as if it was a method on the `Raise<Error>` instance.
 * @return An `EitherNel` containing either the error raised by the `block` or the result of the `block`.
 */
@OptIn(ExperimentalTypeInference::class)
inline fun <Error, A> wrapEitherNel(
    @BuilderInference block: Raise<Error>.() -> A
): EitherNel<Error, A> = either { block(RaiseAccumulate(this)) }

/**
 * This function is an extension function on the generic type `A` and is used to wrap a block of code that can raise an
 * error into an `EitherNel` type.
 * `EitherNel` is a type from the Arrow library that represents a value of one of two possible types (a disjoint union).
 * An instance of `EitherNel` is either an instance of `Left` or `Right`.
 * If the `block` raises an error, the function will return a `Left` containing the error. If the `block` completes
 * successfully, the function will return a `Right` containing the result.
 *
 * @param Error The type of the error that can be raised.
 * @param A The type of the result of the `block`.
 * @param block A block of code that can raise an error. This block is a lambda with receiver, meaning it can be called
 * as if it was a method on the `Raise<Error>` instance.
 * @return An `EitherNel` containing either the error raised by the `block` or the result of the `block`.
 */
@OptIn(ExperimentalTypeInference::class)
inline fun <Error, A> A.applyWrapEitherNel(
    @BuilderInference block: Raise<Error>.() -> Unit
): EitherNel<Error, A> =
    wrapEitherNel {
        block(this)
        this@applyWrapEitherNel
    }

/**
 * This function is used to zip or accumulate results of actions. It takes two actions and a block of code
 * as parameters.
 * Each action is a function that returns an `EitherNel` type. `EitherNel` is a type from the Arrow library that
 * represents a value of one of two possible types (a disjoint union).
 * An instance of `EitherNel` is either an instance of `Left` or `Right`.
 * If an action raises an error, the function will return a `Left` containing the error. If an action completes
 * successfully, the function will return a `Right` containing the result.
 * The block of code is a function that takes two parameters of the successful result types of the actions and returns
 * a result of type `C`.
 * If both actions are successful, the block of code is called with the results of the actions and the result is
 * wrapped in a `Right`.
 * If one or both actions raise an error, the function will return a `Left` containing the accumulated errors.
 *
 * @param Error The type of the error that can be raised.
 * @param A The type of the result of the first action.
 * @param B The type of the result of the second action.
 * @param C The type of the result of the block of code.
 * @param action1 The first action that can raise an error.
 * @param action2 The second action that can raise an error.
 * @param block A block of code that takes the results of the actions and returns a result.
 * @return An `EitherNel` containing either the accumulated errors raised by the actions or the result of the
 * block of code.
 */
@OptIn(ExperimentalContracts::class, ExperimentalTypeInference::class)
inline fun <Error, A, B, C> zipOrAccumulateMerging(
    @BuilderInference action1: () -> EitherNel<Error, A>,
    @BuilderInference action2: () -> EitherNel<Error, B>,
    block: (A, B) -> C
): EitherNel<Error, C> {
    contract { callsInPlace(block, AtMostOnce) }
    val errors = mutableListOf<Error>()
    val a1 = action1().onLeft { errors.addAll(it) }.getOrNull()
    val a2 = action2().onLeft { errors.addAll(it) }.getOrNull()
    return errors.toNonEmptyListOrNone().fold({ block(a1!!, a2!!).right() }, { it.left() })
}

/**
 * This function is used to zip or accumulate results of actions. It takes two actions and a block of code
 * as parameters.
 * Each action is a function that returns an `EitherNel` type. `EitherNel` is a type from the Arrow library that
 * represents a value of one of two possible types (a disjoint union).
 * An instance of `EitherNel` is either an instance of `Left` or `Right`.
 * If an action raises an error, the function will return a `Left` containing the error. If an action completes
 * successfully, the function will return a `Right` containing the result.
 * The block of code is a function that takes two parameters of the successful result types of the actions and returns
 * a result of type `D`.
 * If both actions are successful, the block of code is called with the results of the actions and the result is
 * wrapped in a `Right`.
 * If one or both actions raise an error, the function will return a `Left` containing the accumulated errors.
 *
 * @param Error The type of the error that can be raised.
 * @param A The type of the result of the first action.
 * @param B The type of the result of the second action.
 * @param C The type of the result of the third action.
 * @param D The type of the result of the block of code.
 * @param action1 The first action that can raise an error.
 * @param action2 The second action that can raise an error.
 * @param action3 The third action that can raise an error.
 * @param block A block of code that takes the results of the actions and returns a result.
 * @return An `EitherNel` containing either the accumulated errors raised by the actions or the result of the
 * block of code.
 */
@OptIn(ExperimentalContracts::class, ExperimentalTypeInference::class)
inline fun <Error, A, B, C, D> zipOrAccumulateMerging(
    @BuilderInference action1: () -> EitherNel<Error, A>,
    @BuilderInference action2: () -> EitherNel<Error, B>,
    @BuilderInference action3: () -> EitherNel<Error, C>,
    block: (A, B, C) -> D
): EitherNel<Error, D> {
    contract { callsInPlace(block, AtMostOnce) }
    val errors = mutableListOf<Error>()
    val a1 = action1().onLeft { errors.addAll(it) }.getOrNull()
    val a2 = action2().onLeft { errors.addAll(it) }.getOrNull()
    val a3 = action3().onLeft { errors.addAll(it) }.getOrNull()
    return errors.toNonEmptyListOrNone().fold({ block(a1!!, a2!!, a3!!).right() }, { it.left() })
}

/**
 * This function is used to zip or accumulate results of actions. It takes two actions and a block of code
 * as parameters.
 * Each action is a function that returns an `EitherNel` type. `EitherNel` is a type from the Arrow library that
 * represents a value of one of two possible types (a disjoint union).
 * An instance of `EitherNel` is either an instance of `Left` or `Right`.
 * If an action raises an error, the function will return a `Left` containing the error. If an action completes
 * successfully, the function will return a `Right` containing the result.
 * The block of code is a function that takes two parameters of the successful result types of the actions and returns
 * a result of type `E`.
 * If both actions are successful, the block of code is called with the results of the actions and the result is
 * wrapped in a `Right`.
 * If one or both actions raise an error, the function will return a `Left` containing the accumulated errors.
 *
 * @param Error The type of the error that can be raised.
 * @param A The type of the result of the first action.
 * @param B The type of the result of the second action.
 * @param C The type of the result of the third action.
 * @param D The type of the result of the fourth action.
 * @param E The type of the result of the block of code.
 * @param action1 The first action that can raise an error.
 * @param action2 The second action that can raise an error.
 * @param action3 The third action that can raise an error.
 * @param action4 The fourth action that can raise an error.
 * @param block A block of code that takes the results of the actions and returns a result.
 * @return An `EitherNel` containing either the accumulated errors raised by the actions or the result of the
 * block of code.
 */
@OptIn(ExperimentalContracts::class, ExperimentalTypeInference::class)
inline fun <Error, A, B, C, D, E> zipOrAccumulateMerging(
    @BuilderInference action1: () -> EitherNel<Error, A>,
    @BuilderInference action2: () -> EitherNel<Error, B>,
    @BuilderInference action3: () -> EitherNel<Error, C>,
    @BuilderInference action4: () -> EitherNel<Error, D>,
    block: (A, B, C, D) -> E
): EitherNel<Error, E> {
    contract { callsInPlace(block, AtMostOnce) }
    val errors = mutableListOf<Error>()
    val a1 = action1().onLeft { errors.addAll(it) }.getOrNull()
    val a2 = action2().onLeft { errors.addAll(it) }.getOrNull()
    val a3 = action3().onLeft { errors.addAll(it) }.getOrNull()
    val a4 = action4().onLeft { errors.addAll(it) }.getOrNull()
    return errors.toNonEmptyListOrNone().fold({ block(a1!!, a2!!, a3!!, a4!!).right() }, { it.left() })
}

/**
 * This function is used to zip or accumulate results of actions. It takes two actions and a block of code
 * as parameters.
 * Each action is a function that returns an `EitherNel` type. `EitherNel` is a type from the Arrow library that
 * represents a value of one of two possible types (a disjoint union).
 * An instance of `EitherNel` is either an instance of `Left` or `Right`.
 * If an action raises an error, the function will return a `Left` containing the error. If an action completes
 * successfully, the function will return a `Right` containing the result.
 * The block of code is a function that takes two parameters of the successful result types of the actions and returns
 * a result of type `F`.
 * If both actions are successful, the block of code is called with the results of the actions and the result is
 * wrapped in a `Right`.
 * If one or both actions raise an error, the function will return a `Left` containing the accumulated errors.
 *
 * @param Error The type of the error that can be raised.
 * @param A The type of the result of the first action.
 * @param B The type of the result of the second action.
 * @param C The type of the result of the third action.
 * @param D The type of the result of the fourth action.
 * @param E The type of the result of the fifth action.
 * @param F The type of the result of the block of code.
 * @param action1 The first action that can raise an error.
 * @param action2 The second action that can raise an error.
 * @param action3 The third action that can raise an error.
 * @param action4 The fourth action that can raise an error.
 * @param action5 The fifth action that can raise an error.
 * @param block A block of code that takes the results of the actions and returns a result.
 * @return An `EitherNel` containing either the accumulated errors raised by the actions or the result of the
 * block of code.
 */
@OptIn(ExperimentalContracts::class, ExperimentalTypeInference::class)
inline fun <Error, A, B, C, D, E, F> zipOrAccumulateMerging(
    @BuilderInference action1: () -> EitherNel<Error, A>,
    @BuilderInference action2: () -> EitherNel<Error, B>,
    @BuilderInference action3: () -> EitherNel<Error, C>,
    @BuilderInference action4: () -> EitherNel<Error, D>,
    @BuilderInference action5: () -> EitherNel<Error, E>,
    block: (A, B, C, D, E) -> F
): EitherNel<Error, F> {
    contract { callsInPlace(block, AtMostOnce) }
    val errors = mutableListOf<Error>()
    val a1 = action1().onLeft { errors.addAll(it) }.getOrNull()
    val a2 = action2().onLeft { errors.addAll(it) }.getOrNull()
    val a3 = action3().onLeft { errors.addAll(it) }.getOrNull()
    val a4 = action4().onLeft { errors.addAll(it) }.getOrNull()
    val a5 = action5().onLeft { errors.addAll(it) }.getOrNull()
    return errors.toNonEmptyListOrNone().fold({ block(a1!!, a2!!, a3!!, a4!!, a5!!).right() }, { it.left() })
}

/**
 * This function is used to zip or accumulate results of actions. It takes two actions and a block of code
 * as parameters.
 * Each action is a function that returns an `EitherNel` type. `EitherNel` is a type from the Arrow library that
 * represents a value of one of two possible types (a disjoint union).
 * An instance of `EitherNel` is either an instance of `Left` or `Right`.
 * If an action raises an error, the function will return a `Left` containing the error. If an action completes
 * successfully, the function will return a `Right` containing the result.
 * The block of code is a function that takes two parameters of the successful result types of the actions and returns
 * a result of type `G`.
 * If both actions are successful, the block of code is called with the results of the actions and the result is
 * wrapped in a `Right`.
 * If one or both actions raise an error, the function will return a `Left` containing the accumulated errors.
 *
 * @param Error The type of the error that can be raised.
 * @param A The type of the result of the first action.
 * @param B The type of the result of the second action.
 * @param C The type of the result of the third action.
 * @param D The type of the result of the fourth action.
 * @param E The type of the result of the fifth action.
 * @param F The type of the result of the sixth action.
 * @param G The type of the result of the block of code.
 * @param action1 The first action that can raise an error.
 * @param action2 The second action that can raise an error.
 * @param action3 The third action that can raise an error.
 * @param action4 The fourth action that can raise an error.
 * @param action5 The fifth action that can raise an error.
 * @param action6 The sixth action that can raise an error.
 * @param block A block of code that takes the results of the actions and returns a result.
 * @return An `EitherNel` containing either the accumulated errors raised by the actions or the result of the
 * block of code.
 */
@OptIn(ExperimentalContracts::class, ExperimentalTypeInference::class)
inline fun <Error, A, B, C, D, E, F, G> zipOrAccumulateMerging(
    @BuilderInference action1: () -> EitherNel<Error, A>,
    @BuilderInference action2: () -> EitherNel<Error, B>,
    @BuilderInference action3: () -> EitherNel<Error, C>,
    @BuilderInference action4: () -> EitherNel<Error, D>,
    @BuilderInference action5: () -> EitherNel<Error, E>,
    @BuilderInference action6: () -> EitherNel<Error, F>,
    block: (A, B, C, D, E, F) -> G
): EitherNel<Error, G> {
    contract { callsInPlace(block, AtMostOnce) }
    val errors = mutableListOf<Error>()
    val a1 = action1().onLeft { errors.addAll(it) }.getOrNull()
    val a2 = action2().onLeft { errors.addAll(it) }.getOrNull()
    val a3 = action3().onLeft { errors.addAll(it) }.getOrNull()
    val a4 = action4().onLeft { errors.addAll(it) }.getOrNull()
    val a5 = action5().onLeft { errors.addAll(it) }.getOrNull()
    val a6 = action6().onLeft { errors.addAll(it) }.getOrNull()
    return errors.toNonEmptyListOrNone().fold({ block(a1!!, a2!!, a3!!, a4!!, a5!!, a6!!).right() }, { it.left() })
}

/**
 * This function is used to zip or accumulate results of actions. It takes two actions and a block of code
 * as parameters.
 * Each action is a function that returns an `EitherNel` type. `EitherNel` is a type from the Arrow library that
 * represents a value of one of two possible types (a disjoint union).
 * An instance of `EitherNel` is either an instance of `Left` or `Right`.
 * If an action raises an error, the function will return a `Left` containing the error. If an action completes
 * successfully, the function will return a `Right` containing the result.
 * The block of code is a function that takes two parameters of the successful result types of the actions and returns
 * a result of type `H`.
 * If both actions are successful, the block of code is called with the results of the actions and the result is
 * wrapped in a `Right`.
 * If one or both actions raise an error, the function will return a `Left` containing the accumulated errors.
 *
 * @param Error The type of the error that can be raised.
 * @param A The type of the result of the first action.
 * @param B The type of the result of the second action.
 * @param C The type of the result of the third action.
 * @param D The type of the result of the fourth action.
 * @param E The type of the result of the fifth action.
 * @param F The type of the result of the sixth action.
 * @param G The type of the result of the seventh action.
 * @param H The type of the result of the block of code.
 * @param action1 The first action that can raise an error.
 * @param action2 The second action that can raise an error.
 * @param action3 The third action that can raise an error.
 * @param action4 The fourth action that can raise an error.
 * @param action5 The fifth action that can raise an error.
 * @param action6 The sixth action that can raise an error.
 * @param action7 The seventh action that can raise an error.
 * @param block A block of code that takes the results of the actions and returns a result.
 * @return An `EitherNel` containing either the accumulated errors raised by the actions or the result of the
 * block of code.
 */
@OptIn(ExperimentalContracts::class, ExperimentalTypeInference::class)
inline fun <Error, A, B, C, D, E, F, G, H> zipOrAccumulateMerging(
    @BuilderInference action1: () -> EitherNel<Error, A>,
    @BuilderInference action2: () -> EitherNel<Error, B>,
    @BuilderInference action3: () -> EitherNel<Error, C>,
    @BuilderInference action4: () -> EitherNel<Error, D>,
    @BuilderInference action5: () -> EitherNel<Error, E>,
    @BuilderInference action6: () -> EitherNel<Error, F>,
    @BuilderInference action7: () -> EitherNel<Error, G>,
    block: (A, B, C, D, E, F, G) -> H
): EitherNel<Error, H> {
    contract { callsInPlace(block, AtMostOnce) }
    val errors = mutableListOf<Error>()
    val a1 = action1().onLeft { errors.addAll(it) }.getOrNull()
    val a2 = action2().onLeft { errors.addAll(it) }.getOrNull()
    val a3 = action3().onLeft { errors.addAll(it) }.getOrNull()
    val a4 = action4().onLeft { errors.addAll(it) }.getOrNull()
    val a5 = action5().onLeft { errors.addAll(it) }.getOrNull()
    val a6 = action6().onLeft { errors.addAll(it) }.getOrNull()
    val a7 = action7().onLeft { errors.addAll(it) }.getOrNull()
    return errors.toNonEmptyListOrNone()
        .fold({ block(a1!!, a2!!, a3!!, a4!!, a5!!, a6!!, a7!!).right() }, { it.left() })
}

/**
 * This function is used to zip or accumulate results of actions. It takes two actions and a block of code
 * as parameters.
 * Each action is a function that returns an `EitherNel` type. `EitherNel` is a type from the Arrow library that
 * represents a value of one of two possible types (a disjoint union).
 * An instance of `EitherNel` is either an instance of `Left` or `Right`.
 * If an action raises an error, the function will return a `Left` containing the error. If an action completes
 * successfully, the function will return a `Right` containing the result.
 * The block of code is a function that takes two parameters of the successful result types of the actions and returns
 * a result of type `I`.
 * If both actions are successful, the block of code is called with the results of the actions and the result is
 * wrapped in a `Right`.
 * If one or both actions raise an error, the function will return a `Left` containing the accumulated errors.
 *
 * @param Error The type of the error that can be raised.
 * @param A The type of the result of the first action.
 * @param B The type of the result of the second action.
 * @param C The type of the result of the third action.
 * @param D The type of the result of the fourth action.
 * @param E The type of the result of the fifth action.
 * @param F The type of the result of the sixth action.
 * @param G The type of the result of the seventh action.
 * @param H The type of the result of the eight action.
 * @param I The type of the result of the block of code.
 * @param action1 The first action that can raise an error.
 * @param action2 The second action that can raise an error.
 * @param action3 The third action that can raise an error.
 * @param action4 The fourth action that can raise an error.
 * @param action5 The fifth action that can raise an error.
 * @param action6 The sixth action that can raise an error.
 * @param action7 The seventh action that can raise an error.
 * @param action8 The eight action that can raise an error.
 * @param block A block of code that takes the results of the actions and returns a result.
 * @return An `EitherNel` containing either the accumulated errors raised by the actions or the result of the
 * block of code.
 */
@OptIn(ExperimentalContracts::class, ExperimentalTypeInference::class)
inline fun <Error, A, B, C, D, E, F, G, H, I> zipOrAccumulateMerging(
    @BuilderInference action1: () -> EitherNel<Error, A>,
    @BuilderInference action2: () -> EitherNel<Error, B>,
    @BuilderInference action3: () -> EitherNel<Error, C>,
    @BuilderInference action4: () -> EitherNel<Error, D>,
    @BuilderInference action5: () -> EitherNel<Error, E>,
    @BuilderInference action6: () -> EitherNel<Error, F>,
    @BuilderInference action7: () -> EitherNel<Error, G>,
    @BuilderInference action8: () -> EitherNel<Error, H>,
    block: (A, B, C, D, E, F, G, H) -> I
): EitherNel<Error, I> {
    contract { callsInPlace(block, AtMostOnce) }
    val errors = mutableListOf<Error>()
    val a1 = action1().onLeft { errors.addAll(it) }.getOrNull()
    val a2 = action2().onLeft { errors.addAll(it) }.getOrNull()
    val a3 = action3().onLeft { errors.addAll(it) }.getOrNull()
    val a4 = action4().onLeft { errors.addAll(it) }.getOrNull()
    val a5 = action5().onLeft { errors.addAll(it) }.getOrNull()
    val a6 = action6().onLeft { errors.addAll(it) }.getOrNull()
    val a7 = action7().onLeft { errors.addAll(it) }.getOrNull()
    val a8 = action8().onLeft { errors.addAll(it) }.getOrNull()
    return errors.toNonEmptyListOrNone()
        .fold({ block(a1!!, a2!!, a3!!, a4!!, a5!!, a6!!, a7!!, a8!!).right() }, { it.left() })
}

/**
 * This function is used to zip or accumulate results of actions. It takes two actions and a block of code
 * as parameters.
 * Each action is a function that returns an `EitherNel` type. `EitherNel` is a type from the Arrow library that
 * represents a value of one of two possible types (a disjoint union).
 * An instance of `EitherNel` is either an instance of `Left` or `Right`.
 * If an action raises an error, the function will return a `Left` containing the error. If an action completes
 * successfully, the function will return a `Right` containing the result.
 * The block of code is a function that takes two parameters of the successful result types of the actions and returns
 * a result of type `J`.
 * If both actions are successful, the block of code is called with the results of the actions and the result is
 * wrapped in a `Right`.
 * If one or both actions raise an error, the function will return a `Left` containing the accumulated errors.
 *
 * @param Error The type of the error that can be raised.
 * @param A The type of the result of the first action.
 * @param B The type of the result of the second action.
 * @param C The type of the result of the third action.
 * @param D The type of the result of the fourth action.
 * @param E The type of the result of the fifth action.
 * @param F The type of the result of the sixth action.
 * @param G The type of the result of the seventh action.
 * @param H The type of the result of the eight action.
 * @param I The type of the result of the ninth action.
 * @param J The type of the result of the block of code.
 * @param action1 The first action that can raise an error.
 * @param action2 The second action that can raise an error.
 * @param action3 The third action that can raise an error.
 * @param action4 The fourth action that can raise an error.
 * @param action5 The fifth action that can raise an error.
 * @param action6 The sixth action that can raise an error.
 * @param action7 The seventh action that can raise an error.
 * @param action8 The eight action that can raise an error.
 * @param action9 The ninth action that can raise an error.
 * @param block A block of code that takes the results of the actions and returns a result.
 * @return An `EitherNel` containing either the accumulated errors raised by the actions or the result of the
 * block of code.
 */
@OptIn(ExperimentalContracts::class, ExperimentalTypeInference::class)
inline fun <Error, A, B, C, D, E, F, G, H, I, J> zipOrAccumulateMerging(
    @BuilderInference action1: () -> EitherNel<Error, A>,
    @BuilderInference action2: () -> EitherNel<Error, B>,
    @BuilderInference action3: () -> EitherNel<Error, C>,
    @BuilderInference action4: () -> EitherNel<Error, D>,
    @BuilderInference action5: () -> EitherNel<Error, E>,
    @BuilderInference action6: () -> EitherNel<Error, F>,
    @BuilderInference action7: () -> EitherNel<Error, G>,
    @BuilderInference action8: () -> EitherNel<Error, H>,
    @BuilderInference action9: () -> EitherNel<Error, I>,
    block: (A, B, C, D, E, F, G, H, I) -> J
): EitherNel<Error, J> {
    contract { callsInPlace(block, AtMostOnce) }
    val errors = mutableListOf<Error>()
    val a1 = action1().onLeft { errors.addAll(it) }.getOrNull()
    val a2 = action2().onLeft { errors.addAll(it) }.getOrNull()
    val a3 = action3().onLeft { errors.addAll(it) }.getOrNull()
    val a4 = action4().onLeft { errors.addAll(it) }.getOrNull()
    val a5 = action5().onLeft { errors.addAll(it) }.getOrNull()
    val a6 = action6().onLeft { errors.addAll(it) }.getOrNull()
    val a7 = action7().onLeft { errors.addAll(it) }.getOrNull()
    val a8 = action8().onLeft { errors.addAll(it) }.getOrNull()
    val a9 = action9().onLeft { errors.addAll(it) }.getOrNull()
    return errors.toNonEmptyListOrNone()
        .fold({ block(a1!!, a2!!, a3!!, a4!!, a5!!, a6!!, a7!!, a8!!, a9!!).right() }, { it.left() })
}

/**
 * This function is used to zip or accumulate results of actions. It takes two actions and a block of code
 * as parameters.
 * Each action is a function that returns an `EitherNel` type. `EitherNel` is a type from the Arrow library that
 * represents a value of one of two possible types (a disjoint union).
 * An instance of `EitherNel` is either an instance of `Left` or `Right`.
 * If an action raises an error, the function will return a `Left` containing the error. If an action completes
 * successfully, the function will return a `Right` containing the result.
 * The block of code is a function that returns a result of type `C`.
 * If both actions are successful, the block of code is called and the result is wrapped in a `Right`.
 * If one or both actions raise an error, the function will return a `Left` containing the accumulated errors.
 *
 * @param Error The type of the error that can be raised.
 * @param A The type of the result of the first action.
 * @param B The type of the result of the second action.
 * @param C The type of the result of the block of code.
 * @param action1 The first action that can raise an error.
 * @param action2 The second action that can raise an error.
 * @param block A block of code that returns a result.
 * @return An `EitherNel` containing either the accumulated errors raised by the actions or the result of the block
 * of code.
 */
@OptIn(ExperimentalTypeInference::class)
inline fun <Error, A, B, C> getOrAccumulateMerging(
    @BuilderInference action1: () -> EitherNel<Error, A>,
    @BuilderInference action2: () -> EitherNel<Error, B>,
    block: () -> C
): EitherNel<Error, C> =
    zipOrAccumulateMerging(
        action1,
        action2
    ) { _, _ -> block() }

/**
 * This function is used to zip or accumulate results of actions. It takes two actions and a block of code
 * as parameters.
 * Each action is a function that returns an `EitherNel` type. `EitherNel` is a type from the Arrow library that
 * represents a value of one of two possible types (a disjoint union).
 * An instance of `EitherNel` is either an instance of `Left` or `Right`.
 * If an action raises an error, the function will return a `Left` containing the error. If an action completes
 * successfully, the function will return a `Right` containing the result.
 * The block of code is a function that returns a result of type `D`.
 * If both actions are successful, the block of code is called and the result is wrapped in a `Right`.
 * If one or both actions raise an error, the function will return a `Left` containing the accumulated errors.
 *
 * @param Error The type of the error that can be raised.
 * @param A The type of the result of the first action.
 * @param B The type of the result of the second action.
 * @param C The type of the result of the third action.
 * @param D The type of the result of the block of code.
 * @param action1 The first action that can raise an error.
 * @param action2 The second action that can raise an error.
 * @param action3 The third action that can raise an error.
 * @param block A block of code that returns a result.
 * @return An `EitherNel` containing either the accumulated errors raised by the actions or the result of the block
 * of code.
 */
@OptIn(ExperimentalTypeInference::class)
inline fun <Error, A, B, C, D> getOrAccumulateMerging(
    @BuilderInference action1: () -> EitherNel<Error, A>,
    @BuilderInference action2: () -> EitherNel<Error, B>,
    @BuilderInference action3: () -> EitherNel<Error, C>,
    block: () -> D
): EitherNel<Error, D> =
    zipOrAccumulateMerging(
        action1,
        action2,
        action3
    ) { _, _, _ -> block() }

/**
 * This function is used to zip or accumulate results of actions. It takes two actions and a block of code
 * as parameters.
 * Each action is a function that returns an `EitherNel` type. `EitherNel` is a type from the Arrow library that
 * represents a value of one of two possible types (a disjoint union).
 * An instance of `EitherNel` is either an instance of `Left` or `Right`.
 * If an action raises an error, the function will return a `Left` containing the error. If an action completes
 * successfully, the function will return a `Right` containing the result.
 * The block of code is a function that returns a result of type `E`.
 * If both actions are successful, the block of code is called and the result is wrapped in a `Right`.
 * If one or both actions raise an error, the function will return a `Left` containing the accumulated errors.
 *
 * @param Error The type of the error that can be raised.
 * @param A The type of the result of the first action.
 * @param B The type of the result of the second action.
 * @param C The type of the result of the third action.
 * @param D The type of the result of the fourth action.
 * @param E The type of the result of the block of code.
 * @param action1 The first action that can raise an error.
 * @param action2 The second action that can raise an error.
 * @param action3 The third action that can raise an error.
 * @param action4 The fourth action that can raise an error.
 * @param block A block of code that returns a result.
 * @return An `EitherNel` containing either the accumulated errors raised by the actions or the result of the block
 * of code.
 */
@OptIn(ExperimentalTypeInference::class)
inline fun <Error, A, B, C, D, E> getOrAccumulateMerging(
    @BuilderInference action1: () -> EitherNel<Error, A>,
    @BuilderInference action2: () -> EitherNel<Error, B>,
    @BuilderInference action3: () -> EitherNel<Error, C>,
    @BuilderInference action4: () -> EitherNel<Error, D>,
    block: () -> E
): EitherNel<Error, E> =
    zipOrAccumulateMerging(
        action1,
        action2,
        action3,
        action4
    ) { _, _, _, _ -> block() }

/**
 * This function is used to zip or accumulate results of actions. It takes two actions and a block of code
 * as parameters.
 * Each action is a function that returns an `EitherNel` type. `EitherNel` is a type from the Arrow library that
 * represents a value of one of two possible types (a disjoint union).
 * An instance of `EitherNel` is either an instance of `Left` or `Right`.
 * If an action raises an error, the function will return a `Left` containing the error. If an action completes
 * successfully, the function will return a `Right` containing the result.
 * The block of code is a function that returns a result of type `F`.
 * If both actions are successful, the block of code is called and the result is wrapped in a `Right`.
 * If one or both actions raise an error, the function will return a `Left` containing the accumulated errors.
 *
 * @param Error The type of the error that can be raised.
 * @param A The type of the result of the first action.
 * @param B The type of the result of the second action.
 * @param C The type of the result of the third action.
 * @param D The type of the result of the fourth action.
 * @param E The type of the result of the fifth action.
 * @param F The type of the result of the block of code.
 * @param action1 The first action that can raise an error.
 * @param action2 The second action that can raise an error.
 * @param action3 The third action that can raise an error.
 * @param action4 The fourth action that can raise an error.
 * @param action5 The fifth action that can raise an error.
 * @param block A block of code that returns a result.
 * @return An `EitherNel` containing either the accumulated errors raised by the actions or the result of the block
 * of code.
 */
@OptIn(ExperimentalTypeInference::class)
inline fun <Error, A, B, C, D, E, F> getOrAccumulateMerging(
    @BuilderInference action1: () -> EitherNel<Error, A>,
    @BuilderInference action2: () -> EitherNel<Error, B>,
    @BuilderInference action3: () -> EitherNel<Error, C>,
    @BuilderInference action4: () -> EitherNel<Error, D>,
    @BuilderInference action5: () -> EitherNel<Error, E>,
    block: () -> F
): EitherNel<Error, F> =
    zipOrAccumulateMerging(
        action1,
        action2,
        action3,
        action4,
        action5
    ) { _, _, _, _, _ -> block() }

/**
 * This function is used to zip or accumulate results of actions. It takes two actions and a block of code
 * as parameters.
 * Each action is a function that returns an `EitherNel` type. `EitherNel` is a type from the Arrow library that
 * represents a value of one of two possible types (a disjoint union).
 * An instance of `EitherNel` is either an instance of `Left` or `Right`.
 * If an action raises an error, the function will return a `Left` containing the error. If an action completes
 * successfully, the function will return a `Right` containing the result.
 * The block of code is a function that returns a result of type `G`.
 * If both actions are successful, the block of code is called and the result is wrapped in a `Right`.
 * If one or both actions raise an error, the function will return a `Left` containing the accumulated errors.
 *
 * @param Error The type of the error that can be raised.
 * @param A The type of the result of the first action.
 * @param B The type of the result of the second action.
 * @param C The type of the result of the third action.
 * @param D The type of the result of the fourth action.
 * @param E The type of the result of the fifth action.
 * @param F The type of the result of the sixth action.
 * @param G The type of the result of the block of code.
 * @param action1 The first action that can raise an error.
 * @param action2 The second action that can raise an error.
 * @param action3 The third action that can raise an error.
 * @param action4 The fourth action that can raise an error.
 * @param action5 The fifth action that can raise an error.
 * @param action6 The sixth action that can raise an error.
 * @param block A block of code that returns a result.
 * @return An `EitherNel` containing either the accumulated errors raised by the actions or the result of the block
 * of code.
 */
@OptIn(ExperimentalTypeInference::class)
inline fun <Error, A, B, C, D, E, F, G> getOrAccumulateMerging(
    @BuilderInference action1: () -> EitherNel<Error, A>,
    @BuilderInference action2: () -> EitherNel<Error, B>,
    @BuilderInference action3: () -> EitherNel<Error, C>,
    @BuilderInference action4: () -> EitherNel<Error, D>,
    @BuilderInference action5: () -> EitherNel<Error, E>,
    @BuilderInference action6: () -> EitherNel<Error, F>,
    block: () -> G
): EitherNel<Error, G> =
    zipOrAccumulateMerging(
        action1,
        action2,
        action3,
        action4,
        action5,
        action6
    ) { _, _, _, _, _, _ -> block() }

/**
 * This function is used to zip or accumulate results of actions. It takes two actions and a block of code
 * as parameters.
 * Each action is a function that returns an `EitherNel` type. `EitherNel` is a type from the Arrow library that
 * represents a value of one of two possible types (a disjoint union).
 * An instance of `EitherNel` is either an instance of `Left` or `Right`.
 * If an action raises an error, the function will return a `Left` containing the error. If an action completes
 * successfully, the function will return a `Right` containing the result.
 * The block of code is a function that returns a result of type `H`.
 * If both actions are successful, the block of code is called and the result is wrapped in a `Right`.
 * If one or both actions raise an error, the function will return a `Left` containing the accumulated errors.
 *
 * @param Error The type of the error that can be raised.
 * @param A The type of the result of the first action.
 * @param B The type of the result of the second action.
 * @param C The type of the result of the third action.
 * @param D The type of the result of the fourth action.
 * @param E The type of the result of the fifth action.
 * @param F The type of the result of the sixth action.
 * @param G The type of the result of the seventh action.
 * @param H The type of the result of the block of code.
 * @param action1 The first action that can raise an error.
 * @param action2 The second action that can raise an error.
 * @param action3 The third action that can raise an error.
 * @param action4 The fourth action that can raise an error.
 * @param action5 The fifth action that can raise an error.
 * @param action6 The sixth action that can raise an error.
 * @param action7 The sixth seventh that can raise an error.
 * @param block A block of code that returns a result.
 * @return An `EitherNel` containing either the accumulated errors raised by the actions or the result of the block
 * of code.
 */
@OptIn(ExperimentalTypeInference::class)
inline fun <Error, A, B, C, D, E, F, G, H> getOrAccumulateMerging(
    @BuilderInference action1: () -> EitherNel<Error, A>,
    @BuilderInference action2: () -> EitherNel<Error, B>,
    @BuilderInference action3: () -> EitherNel<Error, C>,
    @BuilderInference action4: () -> EitherNel<Error, D>,
    @BuilderInference action5: () -> EitherNel<Error, E>,
    @BuilderInference action6: () -> EitherNel<Error, F>,
    @BuilderInference action7: () -> EitherNel<Error, G>,
    block: () -> H
): EitherNel<Error, H> =
    zipOrAccumulateMerging(
        action1,
        action2,
        action3,
        action4,
        action5,
        action6,
        action7
    ) { _, _, _, _, _, _, _ -> block() }

/**
 * This function is used to zip or accumulate results of actions. It takes two actions and a block of code
 * as parameters.
 * Each action is a function that returns an `EitherNel` type. `EitherNel` is a type from the Arrow library that
 * represents a value of one of two possible types (a disjoint union).
 * An instance of `EitherNel` is either an instance of `Left` or `Right`.
 * If an action raises an error, the function will return a `Left` containing the error. If an action completes
 * successfully, the function will return a `Right` containing the result.
 * The block of code is a function that returns a result of type `I`.
 * If both actions are successful, the block of code is called and the result is wrapped in a `Right`.
 * If one or both actions raise an error, the function will return a `Left` containing the accumulated errors.
 *
 * @param Error The type of the error that can be raised.
 * @param A The type of the result of the first action.
 * @param B The type of the result of the second action.
 * @param C The type of the result of the third action.
 * @param D The type of the result of the fourth action.
 * @param E The type of the result of the fifth action.
 * @param F The type of the result of the sixth action.
 * @param G The type of the result of the seventh action.
 * @param H The type of the result of the eight action.
 * @param I The type of the result of the block of code.
 * @param action1 The first action that can raise an error.
 * @param action2 The second action that can raise an error.
 * @param action3 The third action that can raise an error.
 * @param action4 The fourth action that can raise an error.
 * @param action5 The fifth action that can raise an error.
 * @param action6 The sixth action that can raise an error.
 * @param action7 The sixth seventh that can raise an error.
 * @param action8 The sixth eight that can raise an error.
 * @param block A block of code that returns a result.
 * @return An `EitherNel` containing either the accumulated errors raised by the actions or the result of the block
 * of code.
 */
@OptIn(ExperimentalTypeInference::class)
inline fun <Error, A, B, C, D, E, F, G, H, I> getOrAccumulateMerging(
    @BuilderInference action1: () -> EitherNel<Error, A>,
    @BuilderInference action2: () -> EitherNel<Error, B>,
    @BuilderInference action3: () -> EitherNel<Error, C>,
    @BuilderInference action4: () -> EitherNel<Error, D>,
    @BuilderInference action5: () -> EitherNel<Error, E>,
    @BuilderInference action6: () -> EitherNel<Error, F>,
    @BuilderInference action7: () -> EitherNel<Error, G>,
    @BuilderInference action8: () -> EitherNel<Error, H>,
    block: () -> I
): EitherNel<Error, I> =
    zipOrAccumulateMerging(
        action1,
        action2,
        action3,
        action4,
        action5,
        action6,
        action7,
        action8
    ) { _, _, _, _, _, _, _, _ -> block() }

/**
 * This function is used to zip or accumulate results of actions. It takes two actions and a block of code
 * as parameters.
 * Each action is a function that returns an `EitherNel` type. `EitherNel` is a type from the Arrow library that
 * represents a value of one of two possible types (a disjoint union).
 * An instance of `EitherNel` is either an instance of `Left` or `Right`.
 * If an action raises an error, the function will return a `Left` containing the error. If an action completes
 * successfully, the function will return a `Right` containing the result.
 * The block of code is a function that returns a result of type `J`.
 * If both actions are successful, the block of code is called and the result is wrapped in a `Right`.
 * If one or both actions raise an error, the function will return a `Left` containing the accumulated errors.
 *
 * @param Error The type of the error that can be raised.
 * @param A The type of the result of the first action.
 * @param B The type of the result of the second action.
 * @param C The type of the result of the third action.
 * @param D The type of the result of the fourth action.
 * @param E The type of the result of the fifth action.
 * @param F The type of the result of the sixth action.
 * @param G The type of the result of the seventh action.
 * @param H The type of the result of the eight action.
 * @param I The type of the result of the ninth action.
 * @param J The type of the result of the block of code.
 * @param action1 The first action that can raise an error.
 * @param action2 The second action that can raise an error.
 * @param action3 The third action that can raise an error.
 * @param action4 The fourth action that can raise an error.
 * @param action5 The fifth action that can raise an error.
 * @param action6 The sixth action that can raise an error.
 * @param action7 The sixth seventh that can raise an error.
 * @param action8 The sixth eight that can raise an error.
 * @param action9 The sixth ninth that can raise an error.
 * @param block A block of code that returns a result.
 * @return An `EitherNel` containing either the accumulated errors raised by the actions or the result of the block
 * of code.
 */
@OptIn(ExperimentalTypeInference::class)
inline fun <Error, A, B, C, D, E, F, G, H, I, J> getOrAccumulateMerging(
    @BuilderInference action1: () -> EitherNel<Error, A>,
    @BuilderInference action2: () -> EitherNel<Error, B>,
    @BuilderInference action3: () -> EitherNel<Error, C>,
    @BuilderInference action4: () -> EitherNel<Error, D>,
    @BuilderInference action5: () -> EitherNel<Error, E>,
    @BuilderInference action6: () -> EitherNel<Error, F>,
    @BuilderInference action7: () -> EitherNel<Error, G>,
    @BuilderInference action8: () -> EitherNel<Error, H>,
    @BuilderInference action9: () -> EitherNel<Error, I>,
    block: () -> J
): EitherNel<Error, J> =
    zipOrAccumulateMerging(
        action1, action2, action3, action4, action5, action6, action7, action8, action9
    ) { _, _, _, _, _, _, _, _, _ -> block() }
