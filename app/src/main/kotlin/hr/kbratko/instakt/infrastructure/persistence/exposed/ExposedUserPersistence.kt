package hr.kbratko.instakt.infrastructure.persistence.exposed

import arrow.core.Either
import arrow.core.Either.Companion.catchOrThrow
import arrow.core.Option
import arrow.core.flatMap
import arrow.core.left
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.right
import arrow.core.singleOrNone
import arrow.core.toOption
import hr.kbratko.instakt.domain.DbError.EmailAlreadyExists
import hr.kbratko.instakt.domain.DbError.InvalidRegistrationToken
import hr.kbratko.instakt.domain.DbError.InvalidUsernameOrPassword
import hr.kbratko.instakt.domain.DbError.RegistrationTokenNotConfirmed
import hr.kbratko.instakt.domain.DbError.UserNotFound
import hr.kbratko.instakt.domain.DbError.UsernameAlreadyExists
import hr.kbratko.instakt.domain.DomainError
import hr.kbratko.instakt.domain.conversion.convert
import hr.kbratko.instakt.domain.getOrRaise
import hr.kbratko.instakt.domain.model.Image
import hr.kbratko.instakt.domain.model.RegistrationToken.Status.Confirmed
import hr.kbratko.instakt.domain.model.RegistrationToken.Status.Unconfirmed
import hr.kbratko.instakt.domain.model.User
import hr.kbratko.instakt.domain.persistence.RegistrationTokenPersistence
import hr.kbratko.instakt.domain.persistence.UserPersistence
import hr.kbratko.instakt.domain.security.Token
import hr.kbratko.instakt.domain.toUUIDOrNone
import hr.kbratko.instakt.infrastructure.persistence.exposed.UsersTable.profileSelection
import hr.kbratko.instakt.infrastructure.persistence.exposed.UsersTable.userSelection
import java.util.UUID
import org.jetbrains.exposed.crypt.Encryptor
import org.jetbrains.exposed.crypt.encryptedVarchar
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.postgresql.util.PSQLState.UNIQUE_VIOLATION
import org.jetbrains.exposed.sql.ReferenceOption.CASCADE as Cascade
import org.jetbrains.exposed.sql.SchemaUtils.create as createIfNotExists
import org.springframework.security.crypto.bcrypt.BCrypt.checkpw as checkPassword
import org.springframework.security.crypto.bcrypt.BCrypt.gensalt as generateSalt
import org.springframework.security.crypto.bcrypt.BCrypt.hashpw as hashPassword

private const val USERNAME_UNIQUE_INDEX = "users_username_unique_index"

private const val EMAIL_UNIQUE_INDEX = "users_email_unique_index"

private const val REGISTRATION_TOKEN_ID_INDEX = "users_registration_token_fk_unique_index"

object UsersTable : LongIdTable("users", "user_pk") {
    val username = varchar("username", 50).uniqueIndex(USERNAME_UNIQUE_INDEX)
    val email = varchar("email", 256).uniqueIndex(EMAIL_UNIQUE_INDEX)
    val firstName = varchar("first_name", 50)
    val lastName = varchar("last_name", 50)
    val bio = varchar("bio", 1024).default("")
    val profilePictureId = reference("profile_picture_fk", ImagesTable).nullable()
    val passwordHash = encryptedVarchar(
        "password_hash",
        256,
        Encryptor({ hashPassword(it, generateSalt()) }, { it }, { it })
    )
    val role = enumeration<User.Role>("role")
    val registrationTokenId = reference("registration_token_fk", RegistrationTokensTable)
        .uniqueIndex(REGISTRATION_TOKEN_ID_INDEX)

    val userSelection = ColumnSelection(id, username, email, registrationTokenId, role) {
        User(
            User.Id(this[id].value),
            User.Username(this[username]),
            User.Email(this[email]),
            Token.Register(this[registrationTokenId].value.toString()),
            this[role]
        )
    }

    val profileSelection = ColumnSelection(username, email, firstName, lastName, bio, profilePictureId) {
        User.Profile(
            User.Username(this[username]),
            User.Email(this[email]),
            User.FirstName(this[firstName]),
            User.LastName(this[lastName]),
            User.Bio(this[bio]),
            this[profilePictureId].toOption().map { Image.Id(it.value.toString()) }
        )
    }
}

object SocialMediaLinksTable : LongIdTable("social_media_link_pk") {
    val userId = reference("user_fk", UsersTable, onDelete = Cascade)
    val platform = varchar("platform", 100)
    val url = varchar("url", 256)
}

fun ExposedUserPersistence(db: Database, registrationTokenPersistence: RegistrationTokenPersistence) =
    object : UserPersistence {
        init {
            transaction {
                createIfNotExists(UsersTable)
            }
        }

        override suspend fun insert(user: User.New): Either<DomainError, User> = either {
            ioTransaction(db = db) {
                val insertedRegistrationTokenId = registrationTokenPersistence.insert()

                val id = catchOrThrow<ExposedSQLException, EntityID<Long>> {
                    UsersTable.insertAndGetId {
                        it[username] = user.username.value
                        it[email] = user.email.value
                        it[firstName] = user.firstName.value
                        it[lastName] = user.lastName.value
                        it[passwordHash] = user.password.value
                        it[role] = user.role
                        it[registrationTokenId] = UUID.fromString(insertedRegistrationTokenId.value)
                    }
                }.mapLeft { it.convert(ExposedSQLExceptionToDbErrorConversion) }.bind()

                UsersTable
                    .select(userSelection.columns)
                    .where { UsersTable.id eq id }
                    .single()
                    .convert(userSelection.conversion)
            }
        }

        override suspend fun select(username: User.Username): Option<User> = ioTransaction(db = db) {
            UsersTable
                .select(userSelection.columns)
                .where { UsersTable.username eq username.value }
                .singleOrNone()
                .map { it.convert(userSelection.conversion) }
        }

        override suspend fun select(
            username: User.Username,
            password: User.Password
        ): Either<DomainError, User> = ioTransaction(db = db) {
            (UsersTable innerJoin RegistrationTokensTable)
                .select(
                    UsersTable.id,
                    UsersTable.username,
                    UsersTable.email,
                    UsersTable.passwordHash,
                    UsersTable.role,
                    UsersTable.registrationTokenId,
                    RegistrationTokensTable.confirmedAt
                )
                .where { (UsersTable.username eq username.value) }
                .singleOrNone { checkPassword(password.value, it[UsersTable.passwordHash]) }
                .toEither { InvalidUsernameOrPassword }
                .flatMap {
                    when (it.convert(ResultRowToRegistrationTokenStatusConversion)) {
                        is Confirmed -> it.convert(userSelection.conversion).right()
                        is Unconfirmed -> RegistrationTokenNotConfirmed.left()
                    }
                }
        }

        override suspend fun selectProfile(id: User.Id): Option<User.Profile> = ioTransaction(db = db) {
            UsersTable
                .select(profileSelection.columns)
                .where { UsersTable.id eq id.value }
                .singleOrNone()
                .map { it.convert(profileSelection.conversion) }
        }

        override suspend fun select(id: User.Id): Option<User> = ioTransaction(db = db) {
            UsersTable
                .select(userSelection.columns)
                .where { UsersTable.id eq id.value }
                .singleOrNone()
                .map { it.convert(userSelection.conversion) }
        }

        override suspend fun update(data: User.Edit): Either<DomainError, User.Profile> = either {
            ioTransaction(db = db) {
                val updatedCount = catchOrThrow<ExposedSQLException, Int> {
                    UsersTable.update({ UsersTable.id eq data.id.value }) {
                        it[firstName] = data.firstName.value
                        it[lastName] = data.lastName.value
                        it[bio] = data.bio.value
                    }
                }.mapLeft { it.convert(ExposedSQLExceptionToDbErrorConversion) }.bind()

                ensure(updatedCount > 0) { UserNotFound }

                UsersTable
                    .select(profileSelection.columns)
                    .where { UsersTable.id eq data.id.value }
                    .single()
                    .convert(profileSelection.conversion)
            }
        }

        override suspend fun update(data: User.ChangePassword): Either<DomainError, Unit> = either {
            ioTransaction(db = db) {
                val updatedCount = UsersTable.update({ UsersTable.id eq data.id.value }) {
                    it[passwordHash] = data.newPassword.value
                }

                ensure(updatedCount > 0) { UserNotFound }
            }
        }

        override suspend fun delete(id: User.Id): Either<DomainError, User.Id> = either {
            ioTransaction(db = db) {
                val deletedCount = UsersTable.deleteWhere { UsersTable.id eq id.value }

                ensure(deletedCount > 0) { UserNotFound }

                id
            }
        }

        override suspend fun resetRegistrationToken(email: User.Email): Either<DomainError, User> = either {
            ioTransaction(db = db) {
                val user = UsersTable
                    .select(userSelection.columns)
                    .where { UsersTable.email eq email.value }
                    .singleOrNone()
                    .map { it.convert(userSelection.conversion) }
                    .getOrRaise { UserNotFound }

                resetRegistrationToken(user).bind()
            }
        }

        override suspend fun resetRegistrationToken(token: Token.Register): Either<DomainError, User> = either {
            ioTransaction(db = db) {
                val registrationTokenId = token.value.toUUIDOrNone().toEither { InvalidRegistrationToken }.bind()
                val user = UsersTable
                    .select(userSelection.columns)
                    .where { UsersTable.registrationTokenId eq registrationTokenId }
                    .singleOrNone()
                    .map { it.convert(userSelection.conversion) }
                    .getOrRaise { UserNotFound }

                resetRegistrationToken(user).bind()
            }
        }

        suspend fun resetRegistrationToken(user: User): Either<DomainError, User> = either {
            ioTransaction(db = db) {
                val insertedRegistrationTokenId = registrationTokenPersistence.insert()

                catchOrThrow<ExposedSQLException, Int> {
                    UsersTable.update({ UsersTable.id eq user.id.value }) {
                        it[registrationTokenId] = UUID.fromString(insertedRegistrationTokenId.value)
                    }
                }.mapLeft { it.convert(ExposedSQLExceptionToDbErrorConversion) }.bind()

                registrationTokenPersistence.delete(user.registrationToken).bind()

                user.copy(registrationToken = insertedRegistrationTokenId)
            }
        }
    }

private val ExposedSQLExceptionToDbErrorConversion = ExposedSQLExceptionToDomainErrorConversionScope {
    when (sqlState) {
        UNIQUE_VIOLATION.state -> {
            val message = message?.lowercase() ?: ""
            when {
                message.contains(USERNAME_UNIQUE_INDEX) -> UsernameAlreadyExists
                message.contains(EMAIL_UNIQUE_INDEX) -> EmailAlreadyExists
                else -> error("UserPersistence SQLException caught: ${this.message ?: ""}")
            }
        }

        else -> error("UserPersistence SQLException caught: ${message ?: ""}")
    }
}
