package hr.kbratko.instakt.infrastructure.plugins

import arrow.core.nel
import arrow.core.toNonEmptyListOrNull
import hr.kbratko.instakt.domain.EndpointRequestLimitMet
import hr.kbratko.instakt.domain.RequestError.RequestCouldNotBeProcessed
import hr.kbratko.instakt.domain.UnhandledServerError
import hr.kbratko.instakt.infrastructure.routes.Response.Failure
import hr.kbratko.instakt.infrastructure.routes.toFailure
import hr.kbratko.instakt.infrastructure.security.AuthenticationException
import io.ktor.http.HttpStatusCode
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.Unauthorized
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.ContentTransformationException
import io.ktor.server.plugins.requestvalidation.RequestValidationException
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond

fun Application.configureErrorHandling() {
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            this@configureErrorHandling.log.error(cause.message)
            val failure = UnhandledServerError.nel().toFailure()
            call.respond(failure.code, failure)
        }

        exception<BadRequestException> { call, _ ->
            val failure = RequestCouldNotBeProcessed.nel().toFailure()
            call.respond(failure.code, failure)
        }

        exception<RequestValidationException> { call, cause ->
            val failure = Failure(cause.reasons.toNonEmptyListOrNull()!!, BadRequest)
            call.respond(failure.code, failure)
        }

        exception<ContentTransformationException> { call, _ ->
            val failure = RequestCouldNotBeProcessed.nel().toFailure()
            call.respond(failure.code, RequestCouldNotBeProcessed.nel().toFailure())
        }

        exception<AuthenticationException> { call, cause ->
            val failure = cause.errors.toFailure(Unauthorized)
            call.respond(failure.code, failure)
        }

        status(HttpStatusCode.TooManyRequests) { call, _ ->
            val failure = EndpointRequestLimitMet.nel().toFailure()
            call.respond(failure.code, failure)
        }
    }
}
