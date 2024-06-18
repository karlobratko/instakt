package hr.kbratko.instakt.infrastructure.flows

import hr.kbratko.instakt.domain.model.AuditLog
import hr.kbratko.instakt.domain.persistence.AuditLogPersistence
import hr.kbratko.instakt.domain.utility.Broker
import hr.kbratko.instakt.domain.utility.Collector
import hr.kbratko.instakt.domain.utility.Emitter
import io.ktor.server.application.Application
import io.ktor.server.application.log
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import org.koin.dsl.module

@OptIn(ExperimentalCoroutinesApi::class) val auditLogScope =
    CoroutineScope(Dispatchers.IO.limitedParallelism(4) + CoroutineName("auditLogCollector") + Job())

typealias AuditLogEmitter = Emitter<AuditLog.New>

fun Application.FlowsModule() =
    module {
        single(createdAtStart = true) {
            val auditLogPersistence = get<AuditLogPersistence>()

            val broker = Broker<AuditLog.New>()
            val collector = broker as Collector<AuditLog.New>
            val emitter = broker as AuditLogEmitter

            collector
                .onEach { auditLogPersistence.insert(it) }
                .catch { this@FlowsModule.log.error("Failed to insert AuditLog.") }
                .onCompletion { println("done") }
                .launchIn(auditLogScope)

            emitter
        }
    }