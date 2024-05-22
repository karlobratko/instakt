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
import hr.kbratko.instakt.domain.DbError.EmailAlreadyExists
import hr.kbratko.instakt.domain.DbError.InvalidUsernameOrPassword
import hr.kbratko.instakt.domain.DbError.RegistrationTokenNotConfirmed
import hr.kbratko.instakt.domain.DbError.UserNotFound
import hr.kbratko.instakt.domain.DbError.UsernameAlreadyExists
import hr.kbratko.instakt.domain.DomainError
import hr.kbratko.instakt.domain.conversion.ConversionScope
import hr.kbratko.instakt.domain.conversion.convert
import hr.kbratko.instakt.domain.model.RegistrationToken.Status.Confirmed
import hr.kbratko.instakt.domain.model.RegistrationToken.Status.Unconfirmed
import hr.kbratko.instakt.domain.model.User
import hr.kbratko.instakt.domain.persistence.RegistrationTokenPersistence
import hr.kbratko.instakt.domain.persistence.UserPersistence
import hr.kbratko.instakt.domain.security.Token
import hr.kbratko.instakt.infrastructure.persistence.exposed.UsersTable.id
import java.util.UUID
import org.jetbrains.exposed.crypt.Encryptor
import org.jetbrains.exposed.crypt.encryptedVarchar
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
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

object UsersTable : LongIdTable("users", "user_pk") {
    val username = varchar("username", 50).uniqueIndex(USERNAME_UNIQUE_INDEX)
    val email = varchar("email", 256).uniqueIndex(EMAIL_UNIQUE_INDEX)
    val passwordHash = encryptedVarchar(
        "password_hash",
        256,
        Encryptor({ hashPassword(it, generateSalt()) }, { it }, { it })
    )
    val role = enumeration<User.Role>("role")
    val registrationTokenId = reference("registration_token_fk", RegistrationTokensTable, onDelete = Cascade)
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
                        it[passwordHash] = user.password.value
                        it[role] = user.role
                        it[registrationTokenId] = UUID.fromString(insertedRegistrationTokenId.value)
                    }
                }.mapLeft { it.convert(ExposedSQLExceptionToDbErrorConversion) }.bind()

                UsersTable
                    .selectAll()
                    .where { UsersTable.id eq id }
                    .single()
                    .convert(ResultRowToUserConversion)
            }
        }

        override suspend fun select(username: User.Username): Option<User> = ioTransaction(db = db) {
            UsersTable
                .selectAll()
                .where { UsersTable.username eq username.value }
                .singleOrNone()
                .map { it.convert(ResultRowToUserConversion) }
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
                        is Confirmed -> it.convert(ResultRowToUserConversion).right()
                        is Unconfirmed -> RegistrationTokenNotConfirmed.left()
                    }
                }
        }

        override suspend fun select(id: User.Id): Option<User> = ioTransaction(db = db) {
            UsersTable
                .selectAll()
                .where { UsersTable.id eq id.value }
                .singleOrNone()
                .map { it.convert(ResultRowToUserConversion) }
        }

        override suspend fun update(data: User.Edit): Either<DomainError, User> = either {
            ioTransaction(db = db) {
                val updatedCount = catchOrThrow<ExposedSQLException, Int> {
                    UsersTable.update({ UsersTable.id eq data.id.value }) {
                        it[username] = data.username.value
                        it[email] = data.email.value
                    }
                }.mapLeft { it.convert(ExposedSQLExceptionToDbErrorConversion) }.bind()

                ensure(updatedCount > 0) { UserNotFound }

                UsersTable
                    .selectAll()
                    .where { UsersTable.id eq data.id.value }
                    .single()
                    .convert(ResultRowToUserConversion)
            }
        }

        override suspend fun update(data: User.ChangePassword): Either<DomainError, User> = either {
            ioTransaction(db = db) {
                val user = select(data.username, data.oldPassword).bind()

                val updatedCount = UsersTable.update({ UsersTable.id eq user.id.value }) {
                    it[passwordHash] = data.newPassword.value
                }

                ensure(updatedCount > 0) { UserNotFound }

                UsersTable
                    .selectAll()
                    .where { UsersTable.username eq data.username.value }
                    .single()
                    .convert(ResultRowToUserConversion)
            }
        }

        override suspend fun delete(id: User.Id): Either<DomainError, User.Id> = either {
            ioTransaction(db = db) {
                val deletedCount = UsersTable.deleteWhere { UsersTable.id eq id.value }

                ensure(deletedCount > 0) { UserNotFound }

                id
            }
        }
    }

private typealias ResultRowToUserConversionScope = ConversionScope<ResultRow, User>

private val ResultRowToUserConversion = ResultRowToUserConversionScope {
    User(
        User.Id(this[id].value),
        User.Username(this[UsersTable.username]),
        User.Email(this[UsersTable.email]),
        User.PasswordHash(this[UsersTable.passwordHash]),
        Token.Register(this[UsersTable.registrationTokenId].value.toString()),
        this[UsersTable.role]
    )
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
