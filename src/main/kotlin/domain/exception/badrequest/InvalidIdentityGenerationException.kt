package domain.exception.badrequest

class InvalidIdentityGenerationException(input: String) : BadRequestException("Cannot generate Identity from input: '$input'")
