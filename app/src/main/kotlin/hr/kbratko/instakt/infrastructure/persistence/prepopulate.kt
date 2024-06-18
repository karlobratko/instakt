package hr.kbratko.instakt.infrastructure.persistence

import arrow.core.raise.either
import hr.kbratko.instakt.domain.DomainError
import hr.kbratko.instakt.domain.model.Plan
import hr.kbratko.instakt.domain.model.User
import hr.kbratko.instakt.domain.persistence.RegistrationTokenPersistence
import hr.kbratko.instakt.domain.persistence.UserPersistence
import io.ktor.server.application.Application
import kotlinx.coroutines.runBlocking
import org.koin.ktor.ext.inject

fun Application.prepopulateDatabase() {
    val userPersistence by inject<UserPersistence>()
    val registrationTokenPersistence by inject<RegistrationTokenPersistence>()

    runBlocking {
        either<DomainError, Unit> {
            userPersistence.select(User.Username("admin")).onNone {
                val user = userPersistence.insert(
                    User.New(
                        User.Username("admin"),
                        User.Email("admin"),
                        User.FirstName("admin"),
                        User.LastName("admin"),
                        User.Password("Pa\$\$w0rd"),
                        Plan.Gold,
                        User.Role.Admin
                    )
                ).bind()

                val registrationToken = registrationTokenPersistence.insert(user.id).bind()
                registrationTokenPersistence.confirm(registrationToken).bind()
            }
        }
    }
}
