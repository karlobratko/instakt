package hr.kbratko.instakt.domain.security

import arrow.core.Option

interface TokenCache<T> {
    suspend fun put(token: Token, value: T): Option<T>

    suspend fun get(token: Token): Option<T>
    
    suspend fun revoke(token: Token): Option<T>
}
