package hr.kbratko.instakt.domain.mailing

import arrow.core.Nel
import io.ktor.http.ContentType
import io.ktor.http.withCharset
import java.nio.charset.Charset
import kotlinx.serialization.Serializable

private val UTF8 = Charset.forName("UTF-8")

data class Email(
    val from: Participant,
    val to: Nel<Participant>,
    val subject: Subject,
    val content: Content
) {
    @Serializable
    @JvmInline value class Address(val value: String)

    @Serializable
    data class Participant(val address: Address, val name: Name) {
        @Serializable
        @JvmInline value class Name(val value: String)
    }

    @JvmInline value class Subject(val value: String)

    sealed interface Content {
        val value: String
        val type: ContentType

        data class Html(override val value: String) : Content {
            override val type = ContentType.Text.Html.withCharset(UTF8)
        }

        data class Text(override val value: String) : Content {
            override val type = ContentType.Text.Plain.withCharset(UTF8)
        }
    }
}
