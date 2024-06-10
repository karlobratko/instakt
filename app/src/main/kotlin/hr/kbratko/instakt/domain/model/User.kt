@file:UseSerializers(
    OptionSerializer::class,
)

package hr.kbratko.instakt.domain.model

import arrow.core.Option
import arrow.core.serialization.OptionSerializer
import hr.kbratko.instakt.domain.security.Token
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

@Serializable
data class User(
    val id: Id,
    val username: Username,
    val email: Email,
    val registrationToken: Token.Register,
    val role: Role
) {
    @Serializable
    data class Profile(
        val username: Username,
        val email: Email,
        val firstName: FirstName,
        val lastName: LastName,
        val bio: Bio,
        val photoId: Option<Image.Id>
    )

    data class New(
        val username: Username,
        val email: Email,
        val firstName: FirstName,
        val lastName: LastName,
        val password: Password,
        val role: Role
    )

    data class Edit(
        val id: Id,
        val firstName: FirstName,
        val lastName: LastName,
        val bio: Bio
    )

    data class ChangePassword(
        val id: Id,
        val oldPassword: Password,
        val newPassword: Password
    )

    @Serializable @JvmInline value class Id(val value: Long)

    @Serializable @JvmInline value class Username(val value: String)

    @Serializable @JvmInline value class Email(val value: String)

    @Serializable @JvmInline value class FirstName(val value: String)

    @Serializable @JvmInline value class LastName(val value: String)

    @Serializable @JvmInline value class Bio(val value: String)

    @Serializable @JvmInline value class Password(val value: String)

    @Serializable @JvmInline value class PasswordHash(val value: String)

    enum class Role {
        Admin,
        Regular
    }
}
