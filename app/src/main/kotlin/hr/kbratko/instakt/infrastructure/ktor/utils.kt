package hr.kbratko.instakt.infrastructure.ktor

import arrow.core.Option
import arrow.core.Option.Companion.catch
import arrow.core.none
import arrow.core.toOption
import hr.kbratko.instakt.infrastructure.routes.Response
import io.ktor.http.HttpHeaders
import io.ktor.http.auth.HttpAuthHeader
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.parseAuthorizationHeader
import io.ktor.server.plugins.origin
import io.ktor.server.request.ApplicationRequest
import io.ktor.server.request.host
import io.ktor.server.request.port
import io.ktor.server.request.receiveNullable
import io.ktor.server.response.respond

suspend inline fun <reified T> ApplicationCall.receiveOrNone(): Option<T> =
    catch { receiveNullable<T>() }.fold(ifEmpty = { none() }, ifSome = { it.toOption() })