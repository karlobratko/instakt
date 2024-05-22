package hr.kbratko.instakt.infrastructure.mailing

import hr.kbratko.instakt.domain.mailing.Email.Participant

data class Senders(
    val info: Participant,
    val auth: Participant,
    val noReply: Participant
)
