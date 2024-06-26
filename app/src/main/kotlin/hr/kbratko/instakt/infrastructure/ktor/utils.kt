package hr.kbratko.instakt.infrastructure.ktor

import arrow.core.Either
import arrow.core.Option
import arrow.core.Option.Companion.catch
import arrow.core.none
import arrow.core.toOption
import hr.kbratko.instakt.domain.RequestError.RequestCouldNotBeProcessed
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.Principal
import io.ktor.server.auth.principal
import io.ktor.server.config.ApplicationConfigValue
import io.ktor.server.request.receiveNullable

suspend inline fun <reified T> ApplicationCall.receiveOrNone(): Option<T> =
    catch { receiveNullable<T>() }.fold(ifEmpty = { none() }, ifSome = { it.toOption() })

suspend inline fun <reified T> ApplicationCall.receiveOrLeft(): Either<RequestCouldNotBeProcessed, T> =
    receiveOrNone<T>().toEither { RequestCouldNotBeProcessed }

inline fun <reified P : Principal> ApplicationCall.principalOrNone(): Option<P> = principal<P>().toOption()

inline fun <reified P : Principal> ApplicationCall.principal(): P = principal<P>() ?: error("Requested principal, but it was not present.")

fun ApplicationConfigValue.getInt() = getString().toInt()

fun ApplicationConfigValue.getBoolean() = getString().toBoolean()