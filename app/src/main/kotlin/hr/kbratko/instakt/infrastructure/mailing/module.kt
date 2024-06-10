package hr.kbratko.instakt.infrastructure.mailing

import hr.kbratko.instakt.domain.mailing.Email.Address
import hr.kbratko.instakt.domain.mailing.Email.Participant
import hr.kbratko.instakt.domain.mailing.Email.Participant.Name
import hr.kbratko.instakt.infrastructure.mailing.jakarta.JakartaMailingService
import hr.kbratko.instakt.infrastructure.serialization.Resources
import io.ktor.server.application.Application
import jakarta.mail.Authenticator
import jakarta.mail.PasswordAuthentication
import jakarta.mail.Session
import java.util.Properties
import org.koin.dsl.module

fun Application.MailingModule() =
    module {
        val mail = environment.config.config("mail")

        single {
            val smtp = mail.config("smtp")

            val props = Properties().also {
                it["mail.smtp.auth"] = smtp.property("auth").getString().toBoolean()
                it["mail.smtp.starttls.enable"] = smtp.property("startTls").getString().toBoolean()
                it["mail.smtp.host"] = smtp.property("host").getString()
                it["mail.smtp.port"] = smtp.property("port").getString()
                it["mail.smtp.ssl.trust"] = smtp.property("ssl.trust").getString()
            }

            val secrets: MailingCredentialsConfig = Resources.hocon("secrets/mail.dev.conf")
            Session.getInstance(
                props,
                object : Authenticator() {
                    override fun getPasswordAuthentication() =
                        PasswordAuthentication(secrets.username, secrets.password)
                }
            )
        }

        single(createdAtStart = true) {
            val senders = mail.config("senders")

            Senders(
                info = Participant(
                    address = Address(senders.property("info.address").getString()),
                    name = Name(senders.property("info.name").getString())
                ),
                auth = Participant(
                    address = Address(senders.property("auth.address").getString()),
                    name = Name(senders.property("auth.name").getString())
                ),
                noReply = Participant(
                    address = Address(senders.property("noReply.address").getString()),
                    name = Name(senders.property("noReply.name").getString())
                ),
            )
        }

        single { JakartaMailingService(get()) }
    }
