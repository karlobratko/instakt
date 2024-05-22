package hr.kbratko.instakt.infrastructure.security

import kotlinx.serialization.Serializable

@Serializable data class SecuritySecretConfig(val secret: String)