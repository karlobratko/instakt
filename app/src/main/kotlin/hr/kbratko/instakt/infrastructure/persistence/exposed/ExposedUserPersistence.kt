package hr.kbratko.instakt.infrastructure.persistence.exposed

import arrow.core.Either
import arrow.core.Either.Companion.catchOrThrow
import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import arrow.core.firstOrNone
import arrow.core.flatMap
import arrow.core.left
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.right
import arrow.core.singleOrNone
import arrow.core.toOption
import hr.kbratko.instakt.domain.DbError.EmailAlreadyExists
import hr.kbratko.instakt.domain.DbError.InvalidPassword
import hr.kbratko.instakt.domain.DbError.InvalidUsernameOrPassword
import hr.kbratko.instakt.domain.DbError.PlanHoldPeriodNotExceeded
import hr.kbratko.instakt.domain.DbError.RegistrationTokenNotConfirmed
import hr.kbratko.instakt.domain.DbError.RequestedPlanAlreadyActive
import hr.kbratko.instakt.domain.DbError.UserNotFound
import hr.kbratko.instakt.domain.DbError.UsernameAlreadyExists
import hr.kbratko.instakt.domain.DomainError
import hr.kbratko.instakt.domain.config.DefaultInstantProvider
import hr.kbratko.instakt.domain.conversion.convert
import hr.kbratko.instakt.domain.model.ContentMetadata
import hr.kbratko.instakt.domain.model.Plan
import hr.kbratko.instakt.domain.model.User
import hr.kbratko.instakt.domain.persistence.UserPersistence
import hr.kbratko.instakt.domain.security.Token
import hr.kbratko.instakt.domain.utility.getOrRaise
import hr.kbratko.instakt.domain.utility.toKotlinInstant
import hr.kbratko.instakt.domain.utility.toKotlinInstantOrNone
import hr.kbratko.instakt.domain.utility.toUUIDOrNone
import org.jetbrains.exposed.crypt.Encryptor
import org.jetbrains.exposed.crypt.encryptedVarchar
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SortOrder.DESC
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentTimestamp
import org.jetbrains.exposed.sql.kotlin.datetime.timestampWithTimeZone
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.postgresql.util.PSQLState.UNIQUE_VIOLATION
import kotlin.time.Duration
import org.jetbrains.exposed.sql.ReferenceOption.CASCADE as Cascade
import org.jetbrains.exposed.sql.SchemaUtils.create as createIfNotExists
import org.springframework.security.crypto.bcrypt.BCrypt.checkpw as comparePassword
import org.springframework.security.crypto.bcrypt.BCrypt.gensalt as generateSalt
import org.springframework.security.crypto.bcrypt.BCrypt.hashpw as hashPassword

private const val USERNAME_UNIQUE_INDEX = "users_username_unique_index"

private const val EMAIL_UNIQUE_INDEX = "users_email_unique_index"

object UsersTable : LongIdTable("users", "user_pk") {
    val username = varchar("username", 50).uniqueIndex(USERNAME_UNIQUE_INDEX)
    val email = varchar("email", 256).uniqueIndex(EMAIL_UNIQUE_INDEX)
    val firstName = varchar("first_name", 50)
    val lastName = varchar("last_name", 50)
    val bio = varchar("bio", 1024).default("")
    val profilePictureId = reference("profile_picture_fk", ContentMetadataTable).nullable()
    val passwordHash = encryptedVarchar(
        "password_hash",
        256,
        Encryptor({ hashPassword(it, generateSalt()) }, { it }, { it })
    )
    val role = enumeration<User.Role>("role")
}

object PlansTable : LongIdTable("user_plans", "user_plan_pk") {
    val userId = reference("user_fk", UsersTable.id, onDelete = Cascade)
    val plan = enumeration<Plan>("plan")
    val changedAt = timestampWithTimeZone("changed_at").defaultExpression(CurrentTimestamp())
}

val userSelection = TableSelection(
    UsersTable.id,
    UsersTable.username,
    UsersTable.email,
    UsersTable.role
) {
    User(
        User.Id(this[UsersTable.id].value),
        User.Username(this[UsersTable.username]),
        User.Email(this[UsersTable.email]),
        this[UsersTable.role]
    )
}

val profileSelection = TableSelection(
    UsersTable.username,
    UsersTable.email,
    UsersTable.firstName,
    UsersTable.lastName,
    UsersTable.bio,
    UsersTable.profilePictureId,
    PlansTable.plan
) {
    User.Profile(
        User.Username(this[UsersTable.username]),
        User.Email(this[UsersTable.email]),
        User.FirstName(this[UsersTable.firstName]),
        User.LastName(this[UsersTable.lastName]),
        User.Bio(this[UsersTable.bio]),
        this[UsersTable.profilePictureId].toOption().map { ContentMetadata.Id(it.value.toString()) },
        this[PlansTable.plan]
    )
}

data class UserPersistenceConfig(val planHoldDuration: Duration)

fun ExposedUserPersistence(db: Database, config: UserPersistenceConfig) =
    object : UserPersistence {
        init {
            transaction {
                createIfNotExists(UsersTable)
            }
        }

        override suspend fun insert(user: User.New): Either<DomainError, User> = either {
            ioTransaction(db = db) {
                val userId = catchOrThrow<ExposedSQLException, EntityID<Long>> {
                    UsersTable.insertAndGetId {
                        it[username] = user.username.value
                        it[email] = user.email.value
                        it[firstName] = user.firstName.value
                        it[lastName] = user.lastName.value
                        it[passwordHash] = user.password.value
                        it[role] = user.role
                    }
                }.mapLeft { it.convert(ExposedSQLExceptionToDbErrorConversion) }.bind()

                val planId = PlansTable.insertAndGetId {
                    it[this.userId] = userId
                    it[plan] = user.plan
                }

                (UsersTable innerJoin PlansTable)
                    .select(userSelection.columns)
                    .where { (UsersTable.id eq userId) and (PlansTable.id eq planId) }
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

        override suspend fun select(email: User.Email): Option<User> = ioTransaction(db = db) {
            UsersTable
                .select(userSelection.columns)
                .where { UsersTable.email eq email.value }
                .singleOrNone()
                .map { it.convert(userSelection.conversion) }
        }

        override suspend fun select(token: Token.Register): Option<User> = ioTransaction(db = db) {
            token.value.toUUIDOrNone()
                .flatMap { registrationTokenId ->
                    (UsersTable innerJoin RegistrationTokensTable)
                        .select(userSelection.columns)
                        .where { (RegistrationTokensTable.id eq registrationTokenId) }
                        .singleOrNone()
                        .map { it.convert(userSelection.conversion) }
                }
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
                    RegistrationTokensTable.confirmedAt
                )
                .where { (UsersTable.username eq username.value) }
                .singleOrNone { comparePassword(password.value, it[UsersTable.passwordHash]) }
                .toEither { InvalidUsernameOrPassword }
                .flatMap {
                    when (it[RegistrationTokensTable.confirmedAt].toKotlinInstantOrNone()) {
                        is Some -> it.convert(userSelection.conversion).right()
                        is None -> RegistrationTokenNotConfirmed.left()
                    }
                }
        }

        override suspend fun selectProfile(id: User.Id): Option<User.Profile> = ioTransaction(db = db) {
            (UsersTable innerJoin PlansTable)
                .select(profileSelection.columns)
                .where { UsersTable.id eq id.value }
                .orderBy(PlansTable.changedAt, DESC)
                .firstOrNone()
                .map { it.convert(profileSelection.conversion) }
        }

        override suspend fun selectPlan(id: User.Id): Option<Plan> = ioTransaction(db = db) {
            PlansTable
                .select(PlansTable.plan)
                .where { PlansTable.userId eq id.value }
                .orderBy(PlansTable.changedAt, DESC)
                .firstOrNone()
                .map { it[PlansTable.plan] }
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

                (UsersTable innerJoin PlansTable)
                    .select(profileSelection.columns)
                    .where { UsersTable.id eq data.id.value }
                    .orderBy(PlansTable.changedAt, DESC)
                    .first()
                    .convert(profileSelection.conversion)
            }
        }

        override suspend fun update(data: User.ChangePassword): Either<DomainError, Unit> = either {
            ioTransaction(db = db) {
                val passwordHash = UsersTable
                    .select(UsersTable.passwordHash)
                    .where { UsersTable.id eq data.id.value }
                    .singleOrNone()
                    .map { it[UsersTable.passwordHash] }
                    .getOrRaise { UserNotFound }

                ensure(comparePassword(data.oldPassword.value, passwordHash)) { InvalidPassword }

                val updatedCount = UsersTable.update({ UsersTable.id eq data.id.value }) {
                    it[this.passwordHash] = data.newPassword.value
                }

                ensure(updatedCount > 0) { UserNotFound }
            }
        }

        override suspend fun update(data: User.ResetPassword): Either<DomainError, Unit> = either {
            ioTransaction(db = db) {
                val updatedCount = UsersTable.update({ UsersTable.id eq data.id.value }) {
                    it[passwordHash] = data.newPassword.value
                }

                ensure(updatedCount > 0) { UserNotFound }
            }
        }

        override suspend fun update(data: User.ChangePlan): Either<DomainError, Unit> = either {
            ioTransaction(db = db) {
                val (currentPlan, changedAt) = PlansTable
                    .select(PlansTable.plan, PlansTable.changedAt)
                    .where { PlansTable.userId eq data.id.value }
                    .orderBy(PlansTable.changedAt, DESC)
                    .firstOrNone()
                    .map { it[PlansTable.plan] to it[PlansTable.changedAt].toKotlinInstant() }
                    .getOrRaise { UserNotFound }

                ensure(currentPlan != data.plan) { RequestedPlanAlreadyActive }
                if (currentPlan != Plan.Free) {
                    ensure(DefaultInstantProvider.now() - changedAt >= config.planHoldDuration) { PlanHoldPeriodNotExceeded }
                }

                PlansTable.insert {
                    it[userId] = data.id.value
                    it[plan] = data.plan
                }
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
