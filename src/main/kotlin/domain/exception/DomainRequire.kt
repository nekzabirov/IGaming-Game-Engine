package domain.exception

inline fun domainRequire(condition: Boolean, exception: () -> DomainException) {
    if (!condition) throw exception()
}

inline fun <T : Any> domainRequireNotNull(value: T?, exception: () -> DomainException): T {
    if (value == null) throw exception()
    return value
}
