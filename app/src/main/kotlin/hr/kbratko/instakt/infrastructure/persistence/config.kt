package hr.kbratko.instakt.infrastructure.persistence

import kotlinx.serialization.Serializable

@Serializable data class DatabaseCredentialsConfig(val username: String, val password: String)

@Serializable data class S3CredentialsConfig(val username: String, val password: String)
