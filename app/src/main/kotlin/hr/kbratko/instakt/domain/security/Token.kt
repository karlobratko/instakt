package hr.kbratko.instakt.domain.security

sealed interface Token {

    val value: String

    @JvmInline value class Refresh(override val value: String) : Token

    @JvmInline value class Access(override val value: String) : Token
    
    @JvmInline value class Register(override val value: String) : Token
}
