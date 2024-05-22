package hr.kbratko.instakt.infrastructure.jobs

import hr.kbratko.instakt.domain.persistence.RefreshTokenPersistence

fun RevokeExpiredRefreshTokens(refreshTokenPersistence: RefreshTokenPersistence) =
    Job { refreshTokenPersistence.revokeExpired() }
