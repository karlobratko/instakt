package hr.kbratko.instakt.infrastructure.mailing

import kotlinx.serialization.Serializable

@Serializable data class MailingCredentialsConfig(val username: String, val password: String)
