package hr.kbratko.instakt.infrastructure.routes.account.profile

import hr.kbratko.instakt.domain.persistence.ContentMetadataPersistence
import hr.kbratko.instakt.domain.utility.MemorySize
import hr.kbratko.instakt.infrastructure.ktor.principal
import hr.kbratko.instakt.infrastructure.plugins.jwt
import hr.kbratko.instakt.infrastructure.plugins.permissiveRateLimit
import hr.kbratko.instakt.infrastructure.routes.Response
import hr.kbratko.instakt.infrastructure.security.UserPrincipal
import io.ktor.http.HttpStatusCode
import io.ktor.resources.Resource
import io.ktor.server.application.call
import io.ktor.server.resources.get
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject

@Resource("/plan")
data class Plan(val parent: UserProfile = UserProfile()) {
    @Serializable data class Response(
        val usedStorageInBytes: MemorySize
    )
}

fun Route.plan() {
    val contentMetadataPersistence by inject<ContentMetadataPersistence>()

    permissiveRateLimit {
        jwt {
            get<Plan> {
                val principal = call.principal<UserPrincipal>()
                val usedStorageInBytes = contentMetadataPersistence.sumTotalUploadedData(principal.id)
                Response.Success(Plan.Response(usedStorageInBytes), HttpStatusCode.OK)
                    .let { call.respond(it.code, it) }
            }
        }
    }
}