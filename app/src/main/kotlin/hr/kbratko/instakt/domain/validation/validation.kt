package hr.kbratko.instakt.domain.validation

import arrow.core.EitherNel
import arrow.core.left
import arrow.core.raise.ensure
import arrow.core.right
import arrow.core.toNonEmptyListOrNone
import hr.kbratko.instakt.domain.utility.applyWrapEitherNel
import hr.kbratko.instakt.domain.utility.toEitherNel
import hr.kbratko.instakt.domain.utility.toUUIDOrNone
import java.math.BigDecimal
import kotlinx.datetime.Instant

fun interface ValidationScope<Error, A> {
    fun A.validate(): EitherNel<Error, A>
}

@JvmName("validateExt")
fun <Error, A> A.validate(scope: ValidationScope<Error, A>): EitherNel<Error, A> = validate(this, scope)

fun <Error, A> validate(value: A, scope: ValidationScope<Error, A>): EitherNel<Error, A> =
    with(scope) { value.validate() }

class Validator<Error, A>(private val value: A) {
    val errors: MutableList<Error> = mutableListOf()

    infix fun with(scope: ValidationScope<Error, A>) {
        validate(value, scope).onLeft {
            errors.addAll(it)
        }
    }

    infix fun <B> with(block: (A) -> EitherNel<Error, B>) {
        block(value).onLeft {
            errors.addAll(it)
        }
    }
}

fun <Error, A> validate(value: A): Validator<Error, A> = Validator(value)

fun <Error, A> validate(value: A, block: Validator<Error, A>.() -> Unit): EitherNel<Error, A> =
    Validator<Error, A>(value).run {
        block()
        errors.toNonEmptyListOrNone()
            .fold({ value.right() }, { it.left() })
    }

fun <Error, A, B> validate(value: A, property: (A) -> B, block: Validator<Error, B>.() -> Unit): EitherNel<Error, A> =
    Validator<Error, B>(property(value)).run {
        block()
        errors.toNonEmptyListOrNone()
            .fold({ value.right() }, { it.left() })
    }

typealias StringValidationScope<Error> = ValidationScope<Error, String>

fun <Error> StringMinLengthValidation(min: Int, error: () -> Error) = StringValidationScope {
    applyWrapEitherNel {
        ensure(length >= min, error)
    }
}

fun <Error> StringMaxLengthValidation(max: Int, error: () -> Error) = StringValidationScope {
    applyWrapEitherNel {
        ensure(length <= max, error)
    }
}

fun <Error> StringMatchingPatternValidation(regex: Regex, error: () -> Error) = StringValidationScope {
    applyWrapEitherNel {
        ensure(matches(regex), error)
    }
}

fun <Error> StringNoneSatisfiesPredicateValidation(predicate: (Char) -> Boolean, error: () -> Error) =
    StringValidationScope {
        applyWrapEitherNel {
            ensure(none(predicate), error)
        }
    }

fun <Error> StringAnySatisfiesPredicateValidation(predicate: (Char) -> Boolean, error: () -> Error) =
    StringValidationScope {
        applyWrapEitherNel {
            ensure(any(predicate), error)
        }
    }

fun <Error> StringAllSatisfiesPredicateValidation(predicate: (Char) -> Boolean, error: () -> Error) =
    StringValidationScope {
        applyWrapEitherNel {
            ensure(all(predicate), error)
        }
    }

fun <Error> StringSatisfiesPredicateValidation(predicate: (String) -> Boolean, error: () -> Error) =
    StringValidationScope {
        applyWrapEitherNel {
            ensure(predicate(this@StringValidationScope), error)
        }
    }

fun <Error> StringIsUUIDValidation(error: () -> Error) = StringValidationScope {
    this.toUUIDOrNone().map { this }.toEitherNel { error() }
}

inline fun <Error, reified E : Enum<E>> StringIsEnumValidation(crossinline error: () -> Error) =
    StringValidationScope validation@{
        applyWrapEitherNel {
            ensure(enumValues<E>().toList().map { it.toString() }.contains(this@validation), error)
        }
    }

typealias IntValidationScope<Error> = ValidationScope<Error, Int>

fun <Error> IntInRangeValidation(range: IntRange, error: () -> Error) = IntValidationScope validation@{
    applyWrapEitherNel {
        ensure(this@validation in range) { error() }
    }
}

fun <Error> IntInInclusiveRangeValidation(min: Int, max: Int, error: () -> Error) =
    IntInRangeValidation(min..max, error)

fun <Error> IntInExclusiveRangeValidation(min: Int, maxExclusive: Int, error: () -> Error) =
    IntInRangeValidation(min..<maxExclusive, error)

fun <Error> IntLessThanValidation(max: Int, error: () -> Error) = IntValidationScope validation@{
    applyWrapEitherNel {
        ensure(this@validation < max) { error() }
    }
}

fun <Error> IntLessThanOrEqualValidation(max: Int, error: () -> Error) = IntValidationScope validation@{
    applyWrapEitherNel {
        ensure(this@validation <= max) { error() }
    }
}

fun <Error> IntGreaterThanValidation(min: Int, error: () -> Error) = IntValidationScope validation@{
    applyWrapEitherNel {
        ensure(this@validation > min) { error() }
    }
}

fun <Error> IntGreaterThanOrEqualValidation(min: Int, error: () -> Error) = IntValidationScope validation@{
    applyWrapEitherNel {
        ensure(this@validation >= min) { error() }
    }
}

typealias BigDecimalValidationScope<Error> = ValidationScope<Error, BigDecimal>

fun <Error> BigDecimalLessThanValidation(max: BigDecimal, error: () -> Error) = BigDecimalValidationScope validation@{
    applyWrapEitherNel {
        ensure(this@validation < max) { error() }
    }
}

fun <Error> BigDecimalLessThanOrEqualValidation(max: BigDecimal, error: () -> Error) =
    BigDecimalValidationScope validation@{
        applyWrapEitherNel {
            ensure(this@validation <= max) { error() }
        }
    }

fun <Error> BigDecimalGreaterThanValidation(min: BigDecimal, error: () -> Error) =
    BigDecimalValidationScope validation@{
        applyWrapEitherNel {
            ensure(this@validation > min) { error() }
        }
    }

fun <Error> BigDecimalGreaterThanOrEqualValidation(min: BigDecimal, error: () -> Error) =
    BigDecimalValidationScope validation@{
        applyWrapEitherNel {
            ensure(this@validation >= min) { error() }
        }
    }

typealias ShortValidationScope<Error> = ValidationScope<Error, Short>

fun <Error> ShortLessThanValidation(max: Short, error: () -> Error) = ShortValidationScope validation@{
    applyWrapEitherNel {
        ensure(this@validation < max) { error() }
    }
}

fun <Error> ShortLessThanOrEqualValidation(max: Short, error: () -> Error) = ShortValidationScope validation@{
    applyWrapEitherNel {
        ensure(this@validation <= max) { error() }
    }
}

fun <Error> ShortGreaterThanValidation(min: Short, error: () -> Error) = ShortValidationScope validation@{
    applyWrapEitherNel {
        ensure(this@validation > min) { error() }
    }
}

fun <Error> ShortGreaterThanOrEqualValidation(min: Short, error: () -> Error) = ShortValidationScope validation@{
    applyWrapEitherNel {
        ensure(this@validation >= min) { error() }
    }
}

typealias InstantValidationScope<Error> = ValidationScope<Error, Instant>

fun <Error> InstantBeforeValidation(value: Instant, error: () -> Error) = InstantValidationScope validation@{
    applyWrapEitherNel {
        ensure(this@validation < value) { error() }
    }
}

fun <Error> InstantBeforeOrSameValidation(value: Instant, error: () -> Error) = InstantValidationScope validation@{
    applyWrapEitherNel {
        ensure(this@validation <= value) { error() }
    }
}

fun <Error> InstantAfterValidation(value: Instant, error: () -> Error) = InstantValidationScope validation@{
    applyWrapEitherNel {
        ensure(this@validation > value) { error() }
    }
}

fun <Error> InstantAfterOrSameValidation(value: Instant, error: () -> Error) = InstantValidationScope validation@{
    applyWrapEitherNel {
        ensure(this@validation >= value) { error() }
    }
}

typealias ListValidationScope<Error, T> = ValidationScope<Error, List<T>>

fun <Error, T> ListAllSatisfiesPredicateValidation(predicate: (T) -> Boolean, error: () -> Error) =
    ListValidationScope<Error, T> {
        applyWrapEitherNel {
            ensure(all(predicate), error)
        }
    }

fun <Error, T> ListNotEmptyValidation(error: () -> Error) =
    ListValidationScope<Error, T> {
        applyWrapEitherNel {
            ensure(isNotEmpty(), error)
        }
    }

typealias LongValidationScope<Error> = ValidationScope<Error, Long>

fun <Error> LongLessThanValidation(max: Long, error: () -> Error) = LongValidationScope validation@{
    applyWrapEitherNel {
        ensure(this@validation < max) { error() }
    }
}

fun <Error> LongLessThanOrEqualValidation(max: Long, error: () -> Error) = LongValidationScope validation@{
    applyWrapEitherNel {
        ensure(this@validation <= max) { error() }
    }
}

fun <Error> LongGreaterThanValidation(min: Long, error: () -> Error) = LongValidationScope validation@{
    applyWrapEitherNel {
        ensure(this@validation > min) { error() }
    }
}

fun <Error> LongGreaterThanOrEqualValidation(min: Long, error: () -> Error) = LongValidationScope validation@{
    applyWrapEitherNel {
        ensure(this@validation >= min) { error() }
    }
}