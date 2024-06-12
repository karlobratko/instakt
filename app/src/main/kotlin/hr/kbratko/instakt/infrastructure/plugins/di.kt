package hr.kbratko.instakt.infrastructure.plugins

import hr.kbratko.instakt.infrastructure.content.ContentModule
import hr.kbratko.instakt.infrastructure.mailing.MailingModule
import hr.kbratko.instakt.infrastructure.persistence.PersistenceModule
import hr.kbratko.instakt.infrastructure.security.SecurityModule
import io.ktor.server.application.Application
import org.koin.ktor.plugin.koin
import org.koin.logger.SLF4JLogger

fun Application.configureDi() {
    koin {
        logger(SLF4JLogger())
        modules(
            MailingModule(),
            PersistenceModule(),
            SecurityModule(),
            ContentModule()
        )
    }
}