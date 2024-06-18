package hr.kbratko.instakt.domain.persistence

import arrow.core.Option
import arrow.core.none
import hr.kbratko.instakt.domain.model.AuditLog
import hr.kbratko.instakt.domain.persistence.pagination.Page
import hr.kbratko.instakt.domain.persistence.pagination.Sort

interface AuditLogPersistence {
    suspend fun insert(auditLog: AuditLog.New)

    suspend fun select(filter: AuditLog.Filter, page: Page, sort: Option<Sort> = none()): Set<AuditLog>
}