package hr.kbratko.instakt.domain.mailing

import arrow.core.Either
import hr.kbratko.instakt.domain.DomainError

fun interface MailingService {
    suspend fun send(email: Email): Either<DomainError, Email>
}

suspend fun MailingService.send(template: EmailTemplate): Either<DomainError, Email> = send(template.email())
