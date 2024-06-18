package hr.kbratko.instakt.infrastructure.content

import hr.kbratko.instakt.domain.content.ContentService
import hr.kbratko.instakt.infrastructure.content.tika.TikaContentTypeDetector
import io.ktor.server.application.Application
import org.apache.tika.detect.DefaultDetector
import org.koin.dsl.module

fun Application.ContentModule() =
    module {
        single {
            TikaContentTypeDetector(DefaultDetector())
        }

        single {
            ContentService(get(), get(), get(), get())
        }
    }