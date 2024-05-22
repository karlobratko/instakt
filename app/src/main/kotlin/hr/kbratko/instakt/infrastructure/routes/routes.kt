package hr.kbratko.instakt.infrastructure.routes

import hr.kbratko.instakt.infrastructure.routes.auth.access
import hr.kbratko.instakt.infrastructure.routes.auth.register
import io.ktor.resources.Resource
import io.ktor.server.application.Application
import io.ktor.server.routing.routing

@Resource("/api/v1")
data object Api

fun Application.configureRoutes() {
    routing {
//        swaggerUI(path = "swagger", swaggerFile = "openapi/documentation.yaml") {
//            version = "3.1.0"
//        }
//
//        openAPI(path = "openapi", swaggerFile = "openapi/documentation.yaml")

        register()
        access()
    }
}