package domain.exception.forbidden

import domain.exception.DomainException

sealed class ForbiddenException(message: String) : DomainException(message)
