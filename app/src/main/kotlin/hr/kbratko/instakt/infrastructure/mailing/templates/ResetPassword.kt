package hr.kbratko.instakt.infrastructure.mailing.templates

import arrow.core.nel
import hr.kbratko.instakt.domain.mailing.Email
import hr.kbratko.instakt.domain.mailing.Email.Participant
import hr.kbratko.instakt.domain.mailing.EmailTemplate
import hr.kbratko.instakt.domain.model.User
import kotlinx.html.a
import kotlinx.html.body
import kotlinx.html.div
import kotlinx.html.h1
import kotlinx.html.head
import kotlinx.html.html
import kotlinx.html.meta
import kotlinx.html.p
import kotlinx.html.stream.createHTML

fun ResetPassword(
    from: Participant,
    email: User.Email,
    username: User.Username,
    resetUrl: String
) = EmailTemplate {
    Email(
        from = from,
        to = Participant(
            address = Email.Address(email.value),
            name = Participant.Name(username.value)
        ).nel(),
        subject = Email.Subject("Reset Password"),
        content = Email.Content.Html(
            value = createHTML()
                .html {
                    head {
                        meta {
                            name = "viewport"
                            content = "width=device-width, initial-scale=1.0"
                        }
                        meta {
                            httpEquiv = "Content-Type"
                            content = "text/html; charset=UTF-8"
                        }
                    }
                    body {
                        div {
                            h1 { +"Hello ${username.value}!" }
                            p { +"It seems that you requested password reset. :)" }
                            p {
                                +"Reset your password by following this link "
                                a(href = resetUrl) {
                                    +"link"
                                }
                                +"."
                            }
                        }
                    }
                }
        )
    )
}
