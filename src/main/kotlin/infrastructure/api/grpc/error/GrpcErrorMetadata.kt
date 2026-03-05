package infrastructure.api.grpc.error

import io.grpc.Metadata

/**
 * Metadata keys for structured gRPC error responses.
 * These keys are used to pass error details to clients in gRPC trailers.
 */
object GrpcErrorMetadata {
    /** The domain error code as string (e.g., "NOT_FOUND", "INSUFFICIENT_BALANCE") */
    val ERROR_CODE_KEY: Metadata.Key<String> =
        Metadata.Key.of("x-error-code", Metadata.ASCII_STRING_MARSHALLER)

    /** The numeric error code value for programmatic parsing */
    val ERROR_CODE_VALUE_KEY: Metadata.Key<String> =
        Metadata.Key.of("x-error-code-value", Metadata.ASCII_STRING_MARSHALLER)

    /** The entity type involved (for NotFoundError, DuplicateEntityError) */
    val ENTITY_TYPE_KEY: Metadata.Key<String> =
        Metadata.Key.of("x-entity-type", Metadata.ASCII_STRING_MARSHALLER)

    /** The identifier that caused the error */
    val IDENTIFIER_KEY: Metadata.Key<String> =
        Metadata.Key.of("x-identifier", Metadata.ASCII_STRING_MARSHALLER)

    /** The field name (for ValidationError) */
    val FIELD_KEY: Metadata.Key<String> =
        Metadata.Key.of("x-field", Metadata.ASCII_STRING_MARSHALLER)

    /** Additional context or reason */
    val REASON_KEY: Metadata.Key<String> =
        Metadata.Key.of("x-reason", Metadata.ASCII_STRING_MARSHALLER)

    /** Service name (for ExternalServiceError) */
    val SERVICE_KEY: Metadata.Key<String> =
        Metadata.Key.of("x-service", Metadata.ASCII_STRING_MARSHALLER)

    /** Player ID (for balance/limit errors) */
    val PLAYER_ID_KEY: Metadata.Key<String> =
        Metadata.Key.of("x-player-id", Metadata.ASCII_STRING_MARSHALLER)

    /** Required amount (for balance errors) */
    val REQUIRED_AMOUNT_KEY: Metadata.Key<String> =
        Metadata.Key.of("x-required-amount", Metadata.ASCII_STRING_MARSHALLER)

    /** Available amount (for balance errors) */
    val AVAILABLE_AMOUNT_KEY: Metadata.Key<String> =
        Metadata.Key.of("x-available-amount", Metadata.ASCII_STRING_MARSHALLER)

    /** Limit amount (for SpinLimitExceededError) */
    val LIMIT_KEY: Metadata.Key<String> =
        Metadata.Key.of("x-limit", Metadata.ASCII_STRING_MARSHALLER)

    /** Spin amount (for SpinLimitExceededError) */
    val SPIN_AMOUNT_KEY: Metadata.Key<String> =
        Metadata.Key.of("x-spin-amount", Metadata.ASCII_STRING_MARSHALLER)
}
