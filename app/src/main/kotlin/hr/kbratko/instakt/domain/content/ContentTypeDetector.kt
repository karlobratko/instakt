package hr.kbratko.instakt.domain.content

import arrow.core.Option
import hr.kbratko.instakt.domain.model.Content
import java.io.InputStream

fun interface ContentTypeDetector {
    suspend fun detect(stream: InputStream): Option<Content.Type>
}