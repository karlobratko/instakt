package hr.kbratko.instakt.infrastructure.logging

import io.ktor.server.application.Application
import org.koin.dsl.module

fun Application.LoggingModule() =
    module {
        single { ActionLogger(get()) }
    }