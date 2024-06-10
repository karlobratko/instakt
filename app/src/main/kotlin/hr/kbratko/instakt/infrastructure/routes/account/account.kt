package hr.kbratko.instakt.infrastructure.routes.account

import hr.kbratko.instakt.infrastructure.routes.Api
import io.ktor.resources.Resource

@Resource("/account")
data class Account(val parent: Api = Api)