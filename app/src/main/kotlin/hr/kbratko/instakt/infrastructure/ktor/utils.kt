package hr.kbratko.instakt.infrastructure.ktor

import arrow.core.Option
import arrow.core.Option.Companion.catch
import arrow.core.none
import arrow.core.toOption
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receiveNullable

suspend inline fun <reified T> ApplicationCall.receiveOrNone(): Option<T> =
    catch { receiveNullable<T>() }.fold(ifEmpty = { none() }, ifSome = { it.toOption() })