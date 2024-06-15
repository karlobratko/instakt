package hr.kbratko.instakt.infrastructure.persistence.exposed

import arrow.core.None
import arrow.core.Some
import arrow.core.toOption
import hr.kbratko.instakt.domain.conversion.ConversionScope
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.experimental.withSuspendTransaction
import kotlin.coroutines.CoroutineContext

data class TableSelection<T>(
    val columns: List<Column<*>>,
    val conversion: ConversionScope<ResultRow, T>
) {
    constructor(vararg columns: Column<*>, conversion: ConversionScope<ResultRow, T>) : this(
        columns.toList(),
        conversion
    )
}

suspend fun <T> ioTransaction(
    context: CoroutineContext = Dispatchers.IO,
    db: Database,
    block: suspend Transaction.() -> T
): T = when (val transaction = TransactionManager.currentOrNone()) {
    is Some -> transaction.value.withSuspendTransaction(context = context) { block() }
    is None -> newSuspendedTransaction(context = context, db = db) { block() }
}

fun TransactionManager.Companion.currentOrNone() = currentOrNull().toOption()

inline fun <reified T : Enum<T>> enumToSql() =
    enumValues<T>().joinToString(
        separator = "', '",
        prefix = "ENUM('",
        postfix = "')"
    )

inline fun <reified T : Enum<T>> Transaction.createEnumeration(name: String = "") {
    val enumName: String =
        when {
            name.isNotEmpty() -> name
            javaClass.`package` == null -> javaClass.name.removeSuffix("Enum")
            else -> javaClass.name.removePrefix("${javaClass.`package`.name}.").substringAfter('$').removeSuffix("Enum")
        }
    exec("CREATE TYPE $enumName AS ${enumToSql<T>()}")
}

inline fun <reified T : Enum<T>> Table.customEnumeration(
    name: String,
    noinline fromDb: (Any) -> T,
    noinline toDb: (T) -> Any
) = customEnumeration(name, enumToSql<T>(), fromDb, toDb)

inline fun <reified T : Enum<T>> Table.customEnumeration(name: String) =
    customEnumeration<T>(name, { enumValues<T>()[it as Int] }, { it.ordinal })

inline fun <reified T : Enum<T>> Table.customEnumerationByName(name: String) =
    customEnumeration<T>(name, { enumValueOf<T>(it as String) }, { it.name })
