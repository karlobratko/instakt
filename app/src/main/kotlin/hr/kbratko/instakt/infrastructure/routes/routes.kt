package hr.kbratko.instakt.infrastructure.routes

import hr.kbratko.instakt.infrastructure.routes.account.account
import hr.kbratko.instakt.infrastructure.routes.admin.admin
import hr.kbratko.instakt.infrastructure.routes.auth.auth
import hr.kbratko.instakt.infrastructure.routes.resources.resources
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

        validation()

        auth()
        account()
        resources()
        admin()
    }
}