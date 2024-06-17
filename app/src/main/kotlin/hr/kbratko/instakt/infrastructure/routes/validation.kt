package hr.kbratko.instakt.infrastructure.routes

import arrow.core.EitherNel
import hr.kbratko.instakt.domain.ValidationError
import hr.kbratko.instakt.infrastructure.routes.account.passwordResetValidation
import hr.kbratko.instakt.infrastructure.routes.account.profile.images.profileImagesValidation
import hr.kbratko.instakt.infrastructure.routes.account.profile.passwordValidation
import hr.kbratko.instakt.infrastructure.routes.account.profile.socialValidation
import hr.kbratko.instakt.infrastructure.routes.account.profile.userProfileValidation
import hr.kbratko.instakt.infrastructure.routes.auth.accessValidation
import hr.kbratko.instakt.infrastructure.routes.auth.registerValidation
import hr.kbratko.instakt.infrastructure.routes.resources.images.imagesValidation
import io.ktor.server.application.install
import io.ktor.server.plugins.requestvalidation.RequestValidation
import io.ktor.server.plugins.requestvalidation.ValidationResult.Invalid
import io.ktor.server.plugins.requestvalidation.ValidationResult.Valid
import io.ktor.server.routing.Routing


fun <T> EitherNel<ValidationError, T>.foldValidation() = fold(
    ifLeft = { errors -> Invalid(errors.map { it.toString() }) },
    ifRight = { Valid }
)

fun Routing.validation() {
    install(RequestValidation) {
        registerValidation()
        accessValidation()
        userProfileValidation()
        profileImagesValidation()
        passwordValidation()
        passwordResetValidation()
        socialValidation()
        imagesValidation()
    }
}