@file:UseSerializers(
    OptionSerializer::class,
)

package hr.kbratko.instakt.infrastructure.routes.admin

import arrow.core.Option
import arrow.core.right
import arrow.core.serialization.OptionSerializer
import hr.kbratko.instakt.domain.model.AuditLog
import hr.kbratko.instakt.domain.model.AuditLog.Action
import hr.kbratko.instakt.domain.model.User
import hr.kbratko.instakt.domain.persistence.AuditLogPersistence
import hr.kbratko.instakt.domain.persistence.pagination.Page
import hr.kbratko.instakt.domain.persistence.pagination.Sort
import hr.kbratko.instakt.domain.utility.InstantClosedRange
import hr.kbratko.instakt.domain.validation.AuditLogSortTermIsValid
import hr.kbratko.instakt.domain.validation.InstantRangeIsValid
import hr.kbratko.instakt.domain.validation.PaginationIsValid
import hr.kbratko.instakt.domain.validation.validate
import hr.kbratko.instakt.infrastructure.plugins.admin
import hr.kbratko.instakt.infrastructure.plugins.jwt
import hr.kbratko.instakt.infrastructure.plugins.permissiveRateLimit
import hr.kbratko.instakt.infrastructure.routes.Response
import hr.kbratko.instakt.infrastructure.routes.foldValidation
import io.ktor.http.HttpStatusCode
import io.ktor.resources.Resource
import io.ktor.server.application.call
import io.ktor.server.plugins.requestvalidation.RequestValidationConfig
import io.ktor.server.resources.post
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import org.koin.ktor.ext.inject

@Resource("/logs")
data class Logs(val parent: Admin = Admin()) {
    @Serializable data class Body(
        val filter: Filter,
        val page: Page,
        val sort: Option<Sort>
    ) {
        @Serializable data class Filter(
            val userId: Option<Long>,
            val action: Option<Action>,
            val affectedResource: Option<AuditLog.Resource>,
            val executedBetween: Option<InstantClosedRange>
        )
    }
}

fun RequestValidationConfig.logsValidation() {
    validate<Logs.Body> { request ->
        validate(request) {
            with { request ->
                request.filter.executedBetween
                    .fold(
                        ifEmpty = { request.right() },
                        ifSome = {
                            it.validate(InstantRangeIsValid)
                        }
                    )
            }
            with { request ->
                request.sort
                    .fold(
                        ifEmpty = { request.right() },
                        ifSome = {
                            it.by.validate(AuditLogSortTermIsValid)
                        }
                    )
            }
            with { it.page.validate(PaginationIsValid(50)) }
        }.foldValidation()
    }
}

fun Route.logs() {
    val auditLogPersistence by inject<AuditLogPersistence>()

    permissiveRateLimit {
        jwt {
            admin {
                post<Logs, Logs.Body> { _, body ->
                    val metadata = auditLogPersistence.select(
                        body.filter.run {
                            AuditLog.Filter(
                                userId.map { User.Id(it) },
                                action,
                                affectedResource,
                                executedBetween
                            )
                        },
                        body.page,
                        body.sort
                    )

                    Response.Success(metadata, HttpStatusCode.OK)
                        .let { call.respond(it.code, it) }
                }
            }
        }
    }
}