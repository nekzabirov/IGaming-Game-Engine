package domain.exception.badrequest

class InvalidIdentityFormatException : BadRequestException("Identity must contain only lowercase letters, digits, and '_'. Use '_' instead of spaces.")
