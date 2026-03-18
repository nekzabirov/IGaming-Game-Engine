package domain.exception.badrequest

import domain.model.Platform

class UnsupportedPlatformException(platform: Platform) : BadRequestException("Platform $platform is not supported")
