package hr.kbratko.instakt.infrastructure.security.cache

import arrow.core.Option
import arrow.core.none
import hr.kbratko.instakt.domain.security.Token
import hr.kbratko.instakt.domain.security.TokenCache

val NoOpTokenCache = object : TokenCache<Any> {
    override suspend fun put(token: Token, value: Any): Option<Any> = none()

    override suspend fun get(token: Token): Option<Any> = none()

    override suspend fun revoke(token: Token): Option<Any> = none()
}
