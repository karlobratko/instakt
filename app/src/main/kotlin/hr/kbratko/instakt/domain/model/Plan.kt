package hr.kbratko.instakt.domain.model

import hr.kbratko.instakt.domain.serialization.custom.PlanSerializer
import hr.kbratko.instakt.domain.utility.MemorySize
import hr.kbratko.instakt.domain.utility.gigabytes
import kotlinx.serialization.Serializable

@Serializable(with = PlanSerializer::class)
enum class Plan(
    val maxStorage: MemorySize
) {
    Free(1.gigabytes),
    Pro(10.gigabytes),
    Gold(100.gigabytes)
}