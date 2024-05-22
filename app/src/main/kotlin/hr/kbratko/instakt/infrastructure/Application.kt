package hr.kbratko.instakt.infrastructure

import hr.kbratko.instakt.infrastructure.jobs.jobs
import hr.kbratko.instakt.infrastructure.plugins.installPlugins
import hr.kbratko.instakt.infrastructure.routes.configureRoutes
import io.ktor.server.application.Application
import io.ktor.server.netty.EngineMain

fun main(args: Array<String>): Unit = EngineMain.main(args)

fun Application.module() {
    installPlugins()
    configureRoutes()
    jobs()
}