package hr.kbratko.instakt.infrastructure.persistence.exposed.pagination

import arrow.core.Option
import hr.kbratko.instakt.domain.conversion.ConversionScope
import hr.kbratko.instakt.domain.conversion.convert
import hr.kbratko.instakt.domain.persistence.pagination.Sort
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Expression
import org.jetbrains.exposed.sql.SortOrder

fun Sort.Order.toExposedSortOrder() = when (this) {
    Sort.Order.asc -> SortOrder.ASC
    Sort.Order.desc -> SortOrder.DESC
}

fun Sort.toOrderedExpressions(conversion: ConversionScope<String, Option<Column<*>>>): List<Pair<Expression<*>, SortOrder>> =
    by.convert(conversion).fold({ emptyList() }, { listOf((it to order.toExposedSortOrder())) }) +
    then.fold({ emptyList() }, { it.toOrderedExpressions(conversion) })