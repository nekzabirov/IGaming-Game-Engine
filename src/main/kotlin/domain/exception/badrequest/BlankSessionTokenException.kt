package domain.exception.badrequest

class BlankSessionTokenException : BadRequestException("Session token cannot be blank")
