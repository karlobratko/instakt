package hr.kbratko.instakt.infrastructure.persistence

import aws.sdk.kotlin.runtime.auth.credentials.EnvironmentCredentialsProvider
import aws.sdk.kotlin.services.s3.S3Client
import aws.smithy.kotlin.runtime.net.Host
import aws.smithy.kotlin.runtime.net.Scheme
import aws.smithy.kotlin.runtime.net.url.Url
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import hr.kbratko.instakt.domain.model.Content
import hr.kbratko.instakt.domain.toDuration
import hr.kbratko.instakt.infrastructure.ktor.getBoolean
import hr.kbratko.instakt.infrastructure.ktor.getInt
import hr.kbratko.instakt.infrastructure.persistence.exposed.ExposedContentMetadataPersistence
import hr.kbratko.instakt.infrastructure.persistence.exposed.ExposedPasswordResetTokenPersistence
import hr.kbratko.instakt.infrastructure.persistence.exposed.ExposedRefreshTokenPersistence
import hr.kbratko.instakt.infrastructure.persistence.exposed.ExposedRegistrationTokenPersistence
import hr.kbratko.instakt.infrastructure.persistence.exposed.ExposedSocialMediaLinkPersistence
import hr.kbratko.instakt.infrastructure.persistence.exposed.ExposedUserPersistence
import hr.kbratko.instakt.infrastructure.persistence.exposed.PasswordResetTokenPersistenceConfig
import hr.kbratko.instakt.infrastructure.persistence.exposed.RefreshTokenPersistenceConfig
import hr.kbratko.instakt.infrastructure.persistence.exposed.RegistrationTokenPersistenceConfig
import hr.kbratko.instakt.infrastructure.persistence.s3.ContentPersistenceConfig
import hr.kbratko.instakt.infrastructure.persistence.s3.S3ContentPersistence
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
        val s3 = environment.config.config("s3")
        val auth = environment.config.config("auth")

        single(createdAtStart = true) {
            val secrets: DatabaseCredentialsConfig = Resources.hocon("secrets/db.dev.conf")

            val dataSource = HikariDataSource(
                HikariConfig().apply {
                    jdbcUrl = db.property("jdbcUrl").getString()
                    driverClassName = db.property("dataSource.driverClass").getString()
                    secrets.also {
                        username = it.username
                        password = it.password
                    }
                    maximumPoolSize = db.property("maximumPoolSize").getInt()
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
            val client = s3.config("client")
            val endpoint = client.config("endpoint")
            val secrets: S3CredentialsConfig = Resources.hocon("secrets/s3.dev.conf")

            val s3Client = S3Client {
                endpointUrl = Url.invoke {
                    scheme = Scheme.parse(endpoint.property("scheme").getString())
                    host = Host.parse(endpoint.property("host").getString())
                    port = endpoint.property("port").getInt()
                }
                region = client.property("region").getString()
                forcePathStyle = client.property("forcePathStyle").getBoolean()
                credentialsProvider = EnvironmentCredentialsProvider(
                    mapOf(
                        "AWS_ACCESS_KEY_ID" to secrets.username,
                        "AWS_SECRET_ACCESS_KEY" to secrets.password
                    )::get
                )
            }

            s3Client
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

        single(createdAtStart = true) {
            ContentPersistenceConfig(
                Content.Bucket(s3.property("bucket").getString())
            )
        }

        single { ExposedRegistrationTokenPersistence(get(), get()) }

        single { ExposedUserPersistence(get()) }

        single { ExposedRefreshTokenPersistence(get(), get()) }

        single { ExposedPasswordResetTokenPersistence(get(), get()) }

        single { ExposedSocialMediaLinkPersistence(get()) }

        single { S3ContentPersistence(get(), get()) }

        single { ExposedContentMetadataPersistence(get()) }
    }
