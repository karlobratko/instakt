package hr.kbratko.instakt.domain.security

import kotlinx.serialization.Serializable

sealed interface Token {

    val value: String

    @Serializable @JvmInline value class Refresh(override val value: String) : Token

    @Serializable @JvmInline value class Access(override val value: String) : Token

    @Serializable @JvmInline value class Register(override val value: String) : Token

    @Serializable @JvmInline value class PasswordReset(override val value: String) : Token
}