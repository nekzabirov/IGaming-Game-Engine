package domain.exception.badrequest

import domain.exception.DomainException

sealed class BadRequestException(message: String) : DomainException(message)
