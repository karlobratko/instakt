package hr.kbratko.instakt.domain.mailing

fun interface EmailTemplate {
    fun email(): Email
}
