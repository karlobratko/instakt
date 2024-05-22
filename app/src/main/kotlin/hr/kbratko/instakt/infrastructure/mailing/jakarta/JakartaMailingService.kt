package hr.kbratko.instakt.infrastructure.mailing.jakarta

import arrow.core.Either.Companion.catchOrThrow
import hr.kbratko.instakt.domain.MailingError.CouldNotSendEmail
import hr.kbratko.instakt.domain.mailing.Email
import hr.kbratko.instakt.domain.mailing.MailingService
import jakarta.mail.MessagingException
import jakarta.mail.Session
import jakarta.mail.Transport.send
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun JakartaMailingService(session: Session) = MailingService { email ->
    catchOrThrow<MessagingException, Email> {
        email.also {
            val message = with(EmailToMimeMessageConversion(session)) {
                email.convert()
            }

            withContext(Dispatchers.IO) {
                send(message)
            }
        }
    }.mapLeft { CouldNotSendEmail }
}
