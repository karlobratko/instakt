package hr.kbratko.instakt.domain.validation

import arrow.core.raise.catch
import hr.kbratko.instakt.domain.ValidationError.SocialMediaLinkValidationError.PlatformValidationError
import hr.kbratko.instakt.domain.ValidationError.SocialMediaLinkValidationError.PlatformValidationError.TooLongPlatformName
import hr.kbratko.instakt.domain.ValidationError.SocialMediaLinkValidationError.PlatformValidationError.TooShortPlatformName
import hr.kbratko.instakt.domain.ValidationError.SocialMediaLinkValidationError.UrlValidationError
import hr.kbratko.instakt.domain.ValidationError.SocialMediaLinkValidationError.UrlValidationError.InvalidUrlPattern
import hr.kbratko.instakt.domain.ValidationError.SocialMediaLinkValidationError.UrlValidationError.TooLongUrl
import io.ktor.http.Url

typealias PlatformValidationScope = ValidationScope<PlatformValidationError, String>

val PlatformIsValid = PlatformValidationScope {
    validate(this) {
        with(StringMinLengthValidation(3) { TooShortPlatformName })
        with(StringMaxLengthValidation(50) { TooLongPlatformName })
    }
}

private fun isValidUrl(url: String): Boolean =
    catch({
        Url(url).let {
            it.protocol.name in listOf("http", "https") && it.host.isNotEmpty()
        }
    }) { false }

typealias UrlValidationScope = ValidationScope<UrlValidationError, String>

val UrlIsValid = UrlValidationScope {
    validate(this) {
        with(StringMaxLengthValidation(50) { TooLongUrl })
        with(StringSatisfiesPredicateValidation(::isValidUrl) { InvalidUrlPattern })
    }
}