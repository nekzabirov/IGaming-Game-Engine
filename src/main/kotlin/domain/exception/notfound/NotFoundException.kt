package domain.exception.notfound

import domain.exception.DomainException

sealed class NotFoundException(message: String) : DomainException(message)
