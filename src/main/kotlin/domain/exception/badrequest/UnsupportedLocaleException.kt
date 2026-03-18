package domain.exception.badrequest

import domain.vo.Locale

class UnsupportedLocaleException(locale: Locale) : BadRequestException("Locale $locale is not supported")
