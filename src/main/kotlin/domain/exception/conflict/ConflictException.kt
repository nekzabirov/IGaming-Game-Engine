package domain.exception.conflict

import domain.exception.DomainException

sealed class ConflictException(message: String) : DomainException(message)
