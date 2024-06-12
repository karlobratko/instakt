package hr.kbratko.instakt.infrastructure.content.tika

import arrow.core.Option.Companion.catch
import hr.kbratko.instakt.domain.content.ContentTypeDetector
import hr.kbratko.instakt.domain.model.Content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.tika.detect.Detector
import org.apache.tika.metadata.Metadata

fun TikaContentTypeDetector(detector: Detector) = ContentTypeDetector {
    withContext(Dispatchers.IO) {
        catch { Content.Type.valueOf(detector.detect(it, Metadata()).subtype) }
    }
}