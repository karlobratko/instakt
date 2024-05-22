package hr.kbratko.instakt.infrastructure.plugins

import hr.kbratko.instakt.domain.toDuration
import io.ktor.http.HttpHeaders.Authorization
import io.ktor.http.HttpHeaders.ContentType
import io.ktor.http.HttpHeaders.XForwardedFor
import io.ktor.http.HttpMethod.Companion.Delete
import io.ktor.http.HttpMethod.Companion.Get
import io.ktor.http.HttpMethod.Companion.Head
import io.ktor.http.HttpMethod.Companion.Options
import io.ktor.http.HttpMethod.Companion.Post
import io.ktor.http.HttpMethod.Companion.Put
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.compression.Compression
import io.ktor.server.plugins.compression.deflate
import io.ktor.server.plugins.compression.gzip
import io.ktor.server.plugins.compression.minimumSize
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.defaultheaders.DefaultHeaders
import io.ktor.server.plugins.forwardedheaders.ForwardedHeaders
import io.ktor.server.plugins.forwardedheaders.XForwardedHeaders
import io.ktor.server.plugins.hsts.HSTS
import io.ktor.server.plugins.hsts.maxAgeDuration
import io.ktor.server.plugins.partialcontent.PartialContent

fun Application.configureHttp() {
    val config = environment.config

    install(HSTS) {
        includeSubDomains = true
        maxAgeDuration = config.property("hsts.maxAge").getString().toDuration()
    }
    install(ForwardedHeaders)
    install(XForwardedHeaders) {
        useFirstProxy()
    }
    install(DefaultHeaders) {
        header("X-Engine", "Ktor")
    }
    install(PartialContent)
    install(Compression) {
        gzip {
            priority = 1.0
        }
        deflate {
            priority = 10.0
            minimumSize(1024)
        }
    }
    install(CORS) {
        allowMethod(Head)
        allowMethod(Options)
        allowMethod(Get)
        allowMethod(Post)
        allowMethod(Put)
        allowMethod(Delete)
        allowXHttpMethodOverride()
        allowHeader(XForwardedFor)
        allowHeader(Authorization)
        allowHeader(ContentType)
        allowCredentials = true
        if (this@configureHttp.developmentMode) {
            anyHost()
        }
    }

}
