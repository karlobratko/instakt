package hr.kbratko.instakt.domain.conversion

fun interface ConversionScope<A, B> {
    fun A.convert(): B
}

fun <A, B> A.convert(scope: ConversionScope<A, B>): B = with(scope) { this@convert.convert() }
