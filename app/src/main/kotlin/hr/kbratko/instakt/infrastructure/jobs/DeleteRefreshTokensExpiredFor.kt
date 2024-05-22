package hr.kbratko.instakt.infrastructure.jobs

import hr.kbratko.instakt.domain.persistence.RefreshTokenPersistence
import kotlin.time.Duration

fun DeleteRefreshTokensExpiredFor(
    refreshTokenPersistence: RefreshTokenPersistence,
    duration: Duration
) = Job { refreshTokenPersistence.deleteExpiredFor(duration) }
