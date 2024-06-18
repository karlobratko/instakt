package hr.kbratko.instakt.domain.model

import hr.kbratko.instakt.domain.serialization.custom.PlanSerializer
import kotlinx.serialization.Serializable

@Serializable(with = PlanSerializer::class)
enum class Plan(
    val maxStorageInMegabytes: Int
) {
    Free(1 * 1024),
    Pro(10 * 1024),
    Gold(100 * 1024);

    val maxStorageInBytes: Long
        get() = maxStorageInMegabytes.toLong() * 1024 * 1024
}