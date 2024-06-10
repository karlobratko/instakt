package hr.kbratko.instakt.infrastructure.persistence

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import hr.kbratko.instakt.domain.toDuration
import hr.kbratko.instakt.infrastructure.persistence.exposed.ExposedPasswordResetTokenPersistence
import hr.kbratko.instakt.infrastructure.persistence.exposed.ExposedRefreshTokenPersistence
import hr.kbratko.instakt.infrastructure.persistence.exposed.ExposedRegistrationTokenPersistence
import hr.kbratko.instakt.infrastructure.persistence.exposed.ExposedUserPersistence
import hr.kbratko.instakt.infrastructure.persistence.exposed.PasswordResetTokenPersistenceConfig
import hr.kbratko.instakt.infrastructure.persistence.exposed.RefreshTokenPersistenceConfig
import hr.kbratko.instakt.infrastructure.persistence.exposed.RegistrationTokenPersistenceConfig
import hr.kbratko.instakt.infrastructure.serialization.Resources
import io.ktor.server.application.Application
import java.sql.Connection
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.DatabaseConfig
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.koin.dsl.module

fun Application.PersistenceModule() =
    module {
        val db = environment.config.config("db")
        val auth = environment.config.config("auth")

        single {
            val secrets: DatabaseCredentialsConfig = Resources.hocon("secrets/db.dev.conf")

            val dataSource = HikariDataSource(
                HikariConfig().apply {
                    jdbcUrl = db.property("jdbcUrl").getString()
                    driverClassName = db.property("dataSource.driverClass").getString()
                    secrets.also {
                        username = it.username
                        password = it.password
                    }
                    maximumPoolSize = db.property("maximumPoolSize").getString().toInt()
                }
            )

            val flyway = Flyway.configure().dataSource(dataSource).load()
            flyway.migrate()

            Database.connect(
                datasource = dataSource,
                databaseConfig = DatabaseConfig {
                    sqlLogger = StdOutSqlLogger
                    useNestedTransactions = false
                    defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
                    defaultRepetitionAttempts = 2
                }
            )
        }

        single(createdAtStart = true) {
            RefreshTokenPersistenceConfig(
                auth.property("lasting.refresh").getString().toDuration()
            )
        }

        single(createdAtStart = true) {
            RegistrationTokenPersistenceConfig(
                auth.property("lasting.registration").getString().toDuration()
            )
        }

        single(createdAtStart = true) {
            PasswordResetTokenPersistenceConfig(
                auth.property("lasting.passwordReset").getString().toDuration()
            )
        }

        single { ExposedRegistrationTokenPersistence(get(), get()) }

        single { ExposedUserPersistence(get()) }

        single { ExposedRefreshTokenPersistence(get(), get()) }

        single { ExposedPasswordResetTokenPersistence(get(), get()) }
    }
