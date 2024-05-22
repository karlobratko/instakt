package hr.kbratko.instakt.domain.model

import hr.kbratko.instakt.domain.security.Token

data class User(
    val id: Id,
    val username: Username,
    val email: Email,
    val passwordHash: PasswordHash,
    val registrationToken: Token.Register,
    val role: Role
) {
    class New(
        val username: Username,
        val email: Email,
        val password: Password,
        val role: Role
    )

    class Edit(val id: Id, val username: Username, val email: Email)

    class ChangePassword(
        val username: Username,
        val oldPassword: Password,
        val newPassword: Password
    )

    @JvmInline value class Id(val value: Long)

    @JvmInline value class Username(val value: String)

    @JvmInline value class Email(val value: String)

    @JvmInline value class Password(val value: String)

    @JvmInline value class PasswordHash(val value: String)

    enum class Role {
        Admin,
        User
    }
}
