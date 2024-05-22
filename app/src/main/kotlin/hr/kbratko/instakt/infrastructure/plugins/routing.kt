package hr.kbratko.instakt.infrastructure.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.autohead.AutoHeadResponse
import io.ktor.server.plugins.doublereceive.DoubleReceive
import io.ktor.server.plugins.methodoverride.XHttpMethodOverride
import io.ktor.server.resources.Resources
import io.ktor.server.routing.IgnoreTrailingSlash
import io.ktor.server.routing.Routing

fun Application.configureRouting() {
    install(Routing)
    install(Resources)
    install(AutoHeadResponse)
    install(XHttpMethodOverride)
    install(IgnoreTrailingSlash)
    install(DoubleReceive)
}