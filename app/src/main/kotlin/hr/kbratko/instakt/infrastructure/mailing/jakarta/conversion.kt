package hr.kbratko.instakt.infrastructure.mailing.jakarta

import hr.kbratko.instakt.domain.conversion.ConversionScope
import hr.kbratko.instakt.domain.mailing.Email
import jakarta.mail.Session
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeBodyPart
import jakarta.mail.internet.MimeMessage
import jakarta.mail.internet.MimeMultipart
import jakarta.mail.Message.RecipientType.TO as To

typealias EmailToMimeMessageConversionScope = ConversionScope<Email, MimeMessage>

fun EmailToMimeMessageConversion(session: Session) = EmailToMimeMessageConversionScope {
    MimeMessage(session).also { message ->
        message.setFrom(InternetAddress(from.address.value, from.name.value))
        message.setRecipients(To, to.map { InternetAddress(it.address.value, it.name.value) }.toTypedArray())
        message.subject = subject.value
        message.setContent(
            MimeMultipart(
                MimeBodyPart().also { body ->
                    body.setContent(content.value, content.type.toString())
                }
            )
        )
    }
}
