package hr.kbratko.instakt.infrastructure.routes.auth

import hr.kbratko.instakt.infrastructure.routes.Api
import io.ktor.resources.Resource

@Resource("/auth")
data class Auth(val parent: Api = Api)