package domain.exception.system

import domain.exception.DomainException

sealed class SystemException(message: String) : DomainException(message)
