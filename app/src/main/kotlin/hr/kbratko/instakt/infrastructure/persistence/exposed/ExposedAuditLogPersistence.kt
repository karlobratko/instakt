package hr.kbratko.instakt.infrastructure.persistence.exposed

import arrow.core.Option
import arrow.core.getOrElse
import arrow.core.some
import hr.kbratko.instakt.domain.conversion.ConversionScope
import hr.kbratko.instakt.domain.conversion.convert
import hr.kbratko.instakt.domain.model.AuditLog
import hr.kbratko.instakt.domain.model.User
import hr.kbratko.instakt.domain.persistence.AuditLogPersistence
import hr.kbratko.instakt.domain.persistence.pagination.Page
import hr.kbratko.instakt.domain.persistence.pagination.Sort
import hr.kbratko.instakt.domain.utility.toKotlinInstant
import hr.kbratko.instakt.infrastructure.persistence.exposed.pagination.toOrderedExpressions
import java.time.ZoneOffset.UTC
import kotlinx.datetime.toJavaInstant
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentTimestamp
import org.jetbrains.exposed.sql.kotlin.datetime.timestampWithTimeZone
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.ReferenceOption.CASCADE as Cascade
import org.jetbrains.exposed.sql.SchemaUtils.create as createIfNotExists

object AuditLogTable : UUIDTable("audit_log", "audit_log_pk") {
    val userId = reference("user_fk", UsersTable, onDelete = Cascade)
    val action = enumeration<AuditLog.Action>("action")
    val affectedResource = enumeration<AuditLog.Resource>("affected_resource")
    val executedAt = timestampWithTimeZone("executed_at").defaultExpression(CurrentTimestamp())
}

val logSelection = TableSelection(
    AuditLogTable.id,
    AuditLogTable.userId,
    AuditLogTable.action,
    AuditLogTable.affectedResource,
    AuditLogTable.executedAt
) {
    AuditLog(
        AuditLog.Id(this[AuditLogTable.id].value.toString()),
        User.Id(this[AuditLogTable.userId].value),
        this[AuditLogTable.action],
        this[AuditLogTable.affectedResource],
        this[AuditLogTable.executedAt].toKotlinInstant()
    )
}

fun ExposedAuditLogPersistence(db: Database) =
    object : AuditLogPersistence {
        init {
            transaction {
                createIfNotExists(AuditLogTable)
            }
        }

        override suspend fun insert(auditLog: AuditLog.New): Unit = ioTransaction(db = db) {
            AuditLogTable.insert {
                it[userId] = auditLog.userId.value
                it[action] = auditLog.action
                it[affectedResource] = auditLog.affectedResource
            }
        }

        override suspend fun select(filter: AuditLog.Filter, page: Page, sort: Option<Sort>): Set<AuditLog> =
            ioTransaction(db = db) {
                AuditLogTable
                    .select(logSelection.columns)
                    .apply {
                        filter.userId.onSome {
                            andWhere { AuditLogTable.userId eq it.value }
                        }

                        filter.action.onSome {
                            andWhere { AuditLogTable.action eq it }
                        }

                        filter.affectedResource.onSome {
                            andWhere { AuditLogTable.affectedResource eq it }
                        }

                        filter.executedBetween.onSome {
                            andWhere {
                                AuditLogTable.executedAt.between(
                                    it.start.toJavaInstant().atOffset(UTC),
                                    it.endInclusive.toJavaInstant().atOffset(UTC)
                                )
                            }
                        }
                    }
                    .orderBy(
                        *sort.getOrElse { Sort("id") }
                            .toOrderedExpressions(ColumnNameToColumnConversion)
                            .toTypedArray()
                    )
                    .limit(page.count, page.offset)
                    .map { it.convert(logSelection.conversion) }
                    .toSet()
            }
    }

private val ColumnNameToColumnConversion = ConversionScope<String, Option<Column<*>>> {
    when (this) {
        "id" -> AuditLogTable.id.some()
        "userId" -> AuditLogTable.userId.some()
        "action" -> AuditLogTable.action.some()
        "affectedResource" -> AuditLogTable.affectedResource.some()
        "executedAt" -> AuditLogTable.executedAt.some()
        else -> arrow.core.none()
    }
}