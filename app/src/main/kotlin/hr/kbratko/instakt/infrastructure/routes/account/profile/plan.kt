package hr.kbratko.instakt.infrastructure.routes.account.profile

import arrow.core.raise.either
import hr.kbratko.instakt.domain.model.Plan
import hr.kbratko.instakt.domain.model.User
import hr.kbratko.instakt.domain.persistence.ContentMetadataPersistence
import hr.kbratko.instakt.domain.persistence.UserPersistence
import hr.kbratko.instakt.domain.utility.MemorySize
import hr.kbratko.instakt.infrastructure.ktor.principal
import hr.kbratko.instakt.infrastructure.logging.ActionLogger
import hr.kbratko.instakt.infrastructure.plugins.jwt
import hr.kbratko.instakt.infrastructure.plugins.permissiveRateLimit
import hr.kbratko.instakt.infrastructure.routes.Response
import hr.kbratko.instakt.infrastructure.routes.toResponse
import hr.kbratko.instakt.infrastructure.security.UserPrincipal
import io.ktor.http.HttpStatusCode
import io.ktor.resources.Resource
import io.ktor.server.application.call
import io.ktor.server.resources.get
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.resources.put
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject

@Resource("/plan")
data class UserPlan(val parent: UserProfile = UserProfile()) {
    @Serializable data class Body(
        val newPlan: Plan
    )

    @Serializable data class Response(
        val usedStorageInBytes: MemorySize
    )
}

fun Route.plan() {
    val userPersistence by inject<UserPersistence>()
    val contentMetadataPersistence by inject<ContentMetadataPersistence>()
    val actionLogger by inject<ActionLogger>()

    permissiveRateLimit {
        jwt {
            get<UserPlan> {
                val principal = call.principal<UserPrincipal>()
                val usedStorageInBytes = contentMetadataPersistence.sumTotalUploadedData(principal.id)
                Response.Success(UserPlan.Response(usedStorageInBytes), HttpStatusCode.OK)
                    .let { call.respond(it.code, it) }
            }

            put<UserPlan, UserPlan.Body> { _, body ->
                either {
                    val principal = call.principal<UserPrincipal>()
                    userPersistence.update(
                        User.ChangePlan(
                            principal.id,
                            body.newPlan
                        )
                    ).bind()
                    actionLogger.logPlanUpdate(principal.id)
                }.toResponse(HttpStatusCode.OK).let { call.respond(it.code, it) }
            }
        }
    }
}