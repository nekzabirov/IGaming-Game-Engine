package domain.exception.badrequest

/**
 * A date filter that is not a full ISO-8601 instant. Exists so a bad date answers INVALID_ARGUMENT
 * instead of falling out of `Instant.parse` as an unhandled exception, which the gRPC interceptor
 * can only report as INTERNAL.
 */
class InvalidDateFormatException(field: String, value: String) : BadRequestException(
    "Field `$field` must be a full ISO-8601 instant (e.g. 2026-09-01T00:00:00Z), got: `$value`"
)
