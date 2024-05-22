package hr.kbratko.instakt.infrastructure.security.cache

import arrow.core.Option
import arrow.core.toOption
import com.mayakapps.kache.InMemoryKache
import com.mayakapps.kache.KacheStrategy
import hr.kbratko.instakt.domain.security.Token
import hr.kbratko.instakt.domain.security.TokenCache
import kotlin.time.Duration
import kotlin.time.TimeSource

private const val CACHE_MAX_SIZE: Long = 100 * 1024 * 1024 // 100 MB

fun <T : Any> InMemoryTokenCache(
    expireAfter: Duration,
    maxSize: Long = CACHE_MAX_SIZE,
    timeSource: TimeSource = TimeSource.Monotonic
) = object : TokenCache<T> {
    private val cache = InMemoryKache<Token, T>(maxSize = maxSize) {
        strategy = KacheStrategy.LRU
        this.timeSource = timeSource
        expireAfterWriteDuration = expireAfter
    }

    override suspend fun put(token: Token, value: T): Option<T> = cache.put(token, value).toOption()

    override suspend fun get(token: Token): Option<T> = cache.get(token).toOption()

    override suspend fun revoke(token: Token): Option<T> = cache.remove(token).toOption()
}
