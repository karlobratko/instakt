@file:UseSerializers(
    OptionSerializer::class,
)

package hr.kbratko.instakt.domain.persistence.pagination

import arrow.core.Option
import arrow.core.none
import arrow.core.serialization.OptionSerializer
import arrow.core.some
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

@Serializable data class Page(val number: Long, val count: Int) {
    val offset get() = number * count
}

@Serializable
data class Sort(val by: String, val order: Order = Order.asc, val then: Option<Sort> = none()) {
    enum class Order {
        asc, desc
    }
}

infix fun Sort.and(other: Sort): Sort = copy(then = other.some())