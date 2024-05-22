package hr.kbratko.instakt.domain.config

import kotlinx.datetime.Clock.System.now
import kotlinx.datetime.Instant

fun interface InstantProvider {
    fun now(): Instant
}


val DefaultInstantProvider = InstantProvider { now() }


val RoundedInstantProvider = InstantProvider { Instant.fromEpochSeconds(now().epochSeconds) }
