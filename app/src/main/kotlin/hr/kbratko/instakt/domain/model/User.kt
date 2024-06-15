@file:UseSerializers(
    OptionSerializer::class,
)

package hr.kbratko.instakt.domain.model

import arrow.core.Option
import arrow.core.serialization.OptionSerializer
import hr.kbratko.instakt.domain.serialization.custom.UserSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

@Serializable(with = UserSerializer::class)
data class User(
    val id: Id,
    val username: Username,
    val email: Email,
    val role: Role = Role.Regular
) {
    @Serializable
    data class Profile(
        val username: Username,
        val email: Email,
        val firstName: FirstName,
        val lastName: LastName,
        val bio: Bio,
        val photoId: Option<ContentMetadata.Id>
    )

    class New(
        val username: Username,
        val email: Email,
        val firstName: FirstName,
        val lastName: LastName,
        val password: Password,
        val role: Role
    )

    class Edit(
        val id: Id,
        val firstName: FirstName,
        val lastName: LastName,
        val bio: Bio
    )

    class ChangePassword(
        val id: Id,
        val oldPassword: Password,
        val newPassword: Password
    )

    class ResetPassword(
        val id: Id,
        val newPassword: Password
    )

    @Serializable @JvmInline value class Id(val value: Long)

    @Serializable @JvmInline value class Username(val value: String)

    @Serializable @JvmInline value class Email(val value: String)

    @Serializable @JvmInline value class FirstName(val value: String)

    @Serializable @JvmInline value class LastName(val value: String)

    @Serializable @JvmInline value class Bio(val value: String)

    @Serializable @JvmInline value class Password(val value: String)

    enum class Role {
        Admin,
        Regular
    }
}
