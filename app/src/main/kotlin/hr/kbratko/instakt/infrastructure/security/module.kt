package hr.kbratko.instakt.infrastructure.security

import hr.kbratko.instakt.domain.security.Security
import hr.kbratko.instakt.domain.security.SecurityContext
import hr.kbratko.instakt.domain.security.jwt.JwtTokenService
import hr.kbratko.instakt.domain.utility.toDuration
import hr.kbratko.instakt.infrastructure.security.cache.InMemoryTokenCache
import hr.kbratko.instakt.infrastructure.security.cache.NoOpTokenCache
import hr.kbratko.instakt.infrastructure.security.jwt.JwtAccessTokens
import hr.kbratko.instakt.infrastructure.serialization.Resources
import io.ktor.server.application.Application
import org.koin.core.qualifier.named
import org.koin.dsl.module

fun Application.SecurityModule() =
    module {
        val auth = environment.config.config("auth")

        single(createdAtStart = true) {
            val config: SecuritySecretConfig = Resources.hocon("secrets/security.dev.conf")

            Security(
                issuer = auth.property("jwt.issuer").getString(),
                accessLasting = auth.property("lasting.access").getString().toDuration(),
                secret = config.secret
            )
        }

        single(named("kache-cache")) {
            InMemoryTokenCache<SecurityContext>(expireAfter = get<Security>().accessLasting)
        }

        single(named("noop-cache")) { NoOpTokenCache }

        single { JwtAccessTokens }

        single {
            JwtTokenService(
                security = get(),
                algebra = get(),
                refreshTokenPersistence = get(),
                accessTokenCache = get(named("noop-cache"))
            )
        }
    }
