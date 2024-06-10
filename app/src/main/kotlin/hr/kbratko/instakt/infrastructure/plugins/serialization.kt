package hr.kbratko.instakt.infrastructure.plugins

import hr.kbratko.instakt.infrastructure.serialization.custom.UUIDSerializer
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import java.util.UUID
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json(Json {
            isLenient = true
            prettyPrint = true
            ignoreUnknownKeys = true
            serializersModule = SerializersModule {
                contextual(UUID::class, UUIDSerializer)
            }
        })
    }
}