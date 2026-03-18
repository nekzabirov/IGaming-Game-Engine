package domain.exception.badrequest

class InvalidAmountException(value: Long) : BadRequestException("Amount must be non-negative, got: $value")
