package hr.kbratko.instakt.infrastructure.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.origin
import io.ktor.server.plugins.ratelimit.RateLimit
import io.ktor.server.plugins.ratelimit.RateLimitName
import io.ktor.server.plugins.ratelimit.RateLimitProviderConfig
import io.ktor.server.plugins.ratelimit.rateLimit
import io.ktor.server.request.httpMethod
import io.ktor.server.request.uri
import io.ktor.server.routing.Route
import kotlin.time.Duration.Companion.seconds

object RateLimitLevel {
    val Restricted = RateLimitName("restricted")

    val Permissive = RateLimitName("permissive")
}

fun Route.restrictedRateLimit(build: Route.() -> Unit): Route = rateLimit(RateLimitLevel.Restricted, build)

fun Route.permissiveRateLimit(build: Route.() -> Unit): Route = rateLimit(RateLimitLevel.Permissive, build)

fun Application.configureRateLimit() {
    install(RateLimit) {
        global {
            rateLimiter(limit = 30, refillPeriod = 60.seconds)
            limitByClientIpAndRequestHandler()
        }

        register(RateLimitLevel.Permissive) {
            rateLimiter(limit = 20, refillPeriod = 60.seconds)
            limitByClientIpAndRequestHandler()
        }

        register(RateLimitLevel.Restricted) {
            rateLimiter(limit = 10, refillPeriod = 60.seconds)
            limitByClientIpAndRequestHandler()
        }
    }
}

private fun RateLimitProviderConfig.limitByClientIpAndRequestHandler() {
    requestKey { call -> Triple(call.request.origin.remoteHost, call.request.httpMethod, call.request.uri) }
}