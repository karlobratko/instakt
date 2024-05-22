package hr.kbratko.instakt.infrastructure.plugins

import io.ktor.server.application.Application

fun Application.installPlugins() {
    configureDi()
    configureRouting()
    configureSerialization()
    configureHttp()
    configureSecurity()
    configureErrorHandling()
    configureRateLimit()
    configureMonitoring()
}