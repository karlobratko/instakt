package hr.kbratko.instakt.infrastructure.routes.account

import hr.kbratko.instakt.infrastructure.routes.Api
import hr.kbratko.instakt.infrastructure.routes.account.profile.profile
import io.ktor.resources.Resource
import io.ktor.server.routing.Route

@Resource("/account")
data class Account(val parent: Api = Api)

fun Route.account() {
    profile()
    passwordReset()
}