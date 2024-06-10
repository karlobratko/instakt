package hr.kbratko.instakt.domain.security

import hr.kbratko.instakt.domain.toUUIDOrNone
import kotlinx.serialization.Serializable

sealed interface Token {

    val value: String

    @Serializable @JvmInline value class Refresh(override val value: String) : Token

    @Serializable @JvmInline value class Access(override val value: String) : Token

    @Serializable @JvmInline value class Register(override val value: String) : Token
}

fun Token.Register.toUUIDOrNone() = value.toUUIDOrNone()

fun Token.Refresh.toUUIDOrNone() = value.toUUIDOrNone()
