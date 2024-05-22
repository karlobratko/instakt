package hr.kbratko.instakt.infrastructure.persistence.exposed

import hr.kbratko.instakt.domain.DbError
import hr.kbratko.instakt.domain.conversion.ConversionScope
import org.jetbrains.exposed.exceptions.ExposedSQLException

typealias ExposedSQLExceptionToDomainErrorConversionScope = ConversionScope<ExposedSQLException, DbError>
