package infrastructure.api.grpc.error

import domain.common.error.*
import io.grpc.Metadata
import io.grpc.Status
import io.grpc.StatusException
import shared.Logger

/**
 * Centralized mapper for converting DomainError to gRPC StatusException.
 * Ensures consistent error handling across all gRPC services.
 */
object GrpcErrorMapper {

    /**
     * Maps a DomainError to a gRPC StatusException with appropriate
     * status code and metadata containing error details.
     */
    fun toStatusException(error: DomainError): StatusException {
        Logger.warn("[GRPC] DomainError code={} message={}", error.errorCode.name, error.message)
        val status = mapToStatus(error)
        val metadata = buildMetadata(error)
        return status.asException(metadata)
    }

    /**
     * Maps a DomainError to the appropriate gRPC Status using the error code enum.
     */
    private fun mapToStatus(error: DomainError): Status {
        val baseStatus = when (error.errorCode) {
            ErrorCode.NOT_FOUND, ErrorCode.ROUND_NOT_FOUND -> Status.NOT_FOUND
            ErrorCode.VALIDATION_ERROR, ErrorCode.INVALID_PRESET -> Status.INVALID_ARGUMENT
            ErrorCode.INSUFFICIENT_BALANCE, ErrorCode.BET_LIMIT_EXCEEDED,
            ErrorCode.ROUND_FINISHED, ErrorCode.ILLEGAL_STATE -> Status.FAILED_PRECONDITION
            ErrorCode.SESSION_INVALID -> Status.UNAUTHENTICATED
            ErrorCode.GAME_UNAVAILABLE, ErrorCode.EXTERNAL_SERVICE_ERROR -> Status.UNAVAILABLE
            ErrorCode.DUPLICATE_ENTITY -> Status.ALREADY_EXISTS
            ErrorCode.AGGREGATOR_NOT_SUPPORTED -> Status.UNIMPLEMENTED
            ErrorCode.AGGREGATOR_ERROR, ErrorCode.INTERNAL_ERROR -> Status.INTERNAL
        }

        return baseStatus.withDescription(error.message).let { status ->
            error.cause?.let { cause -> status.withCause(cause) } ?: status
        }
    }

    /**
     * Builds metadata with error-specific context.
     */
    private fun buildMetadata(error: DomainError): Metadata {
        return Metadata().apply {
            // Add both string code and numeric value for flexibility
            put(GrpcErrorMetadata.ERROR_CODE_KEY, error.errorCode.name)
            put(GrpcErrorMetadata.ERROR_CODE_VALUE_KEY, error.errorCode.value.toString())

            when (error) {
                is NotFoundError -> {
                    put(GrpcErrorMetadata.ENTITY_TYPE_KEY, error.entity)
                    put(GrpcErrorMetadata.IDENTIFIER_KEY, error.identifier)
                }
                is RoundNotFoundError -> {
                    put(GrpcErrorMetadata.ENTITY_TYPE_KEY, "Round")
                    put(GrpcErrorMetadata.IDENTIFIER_KEY, error.roundId)
                }
                is ValidationError -> {
                    put(GrpcErrorMetadata.FIELD_KEY, error.field)
                    put(GrpcErrorMetadata.REASON_KEY, error.reason)
                }
                is InsufficientBalanceError -> {
                    put(GrpcErrorMetadata.PLAYER_ID_KEY, error.playerId)
                    put(GrpcErrorMetadata.REQUIRED_AMOUNT_KEY, error.required.toString())
                    put(GrpcErrorMetadata.AVAILABLE_AMOUNT_KEY, error.available.toString())
                }
                is BetLimitExceededError -> {
                    put(GrpcErrorMetadata.PLAYER_ID_KEY, error.playerId)
                    put(GrpcErrorMetadata.BET_AMOUNT_KEY, error.betAmount.toString())
                    put(GrpcErrorMetadata.LIMIT_KEY, error.limit.toString())
                }
                is SessionInvalidError -> {
                    put(GrpcErrorMetadata.IDENTIFIER_KEY, error.sessionToken)
                    put(GrpcErrorMetadata.REASON_KEY, error.reason)
                }
                is GameUnavailableError -> {
                    put(GrpcErrorMetadata.IDENTIFIER_KEY, error.gameIdentity)
                    put(GrpcErrorMetadata.REASON_KEY, error.reason)
                }
                is RoundFinishedError -> {
                    put(GrpcErrorMetadata.IDENTIFIER_KEY, error.roundId)
                }
                is InvalidPresetError -> {
                    put(GrpcErrorMetadata.IDENTIFIER_KEY, error.presetId)
                    put(GrpcErrorMetadata.REASON_KEY, error.reason)
                }
                is ExternalServiceError -> {
                    put(GrpcErrorMetadata.SERVICE_KEY, error.service)
                    put(GrpcErrorMetadata.REASON_KEY, error.details)
                }
                is IllegalStateError -> {
                    put(GrpcErrorMetadata.FIELD_KEY, error.operation)
                    put(GrpcErrorMetadata.REASON_KEY, error.currentState)
                }
                is DuplicateEntityError -> {
                    put(GrpcErrorMetadata.ENTITY_TYPE_KEY, error.entity)
                    put(GrpcErrorMetadata.IDENTIFIER_KEY, error.identifier)
                }
                is AggregatorNotSupportedError -> {
                    put(GrpcErrorMetadata.IDENTIFIER_KEY, error.aggregator)
                }
                is AggregatorError -> {
                    // Code is already set above, no additional metadata needed
                }
            }
        }
    }

    /**
     * Maps a generic Throwable to a gRPC StatusException.
     * Falls back to INTERNAL for unknown errors.
     */
    fun toStatusException(throwable: Throwable): StatusException {
        return when (throwable) {
            is DomainError -> toStatusException(throwable)
            is StatusException -> throwable
            else -> {
                Logger.error("[GRPC] Unexpected error: ${throwable.message}", throwable)
                val metadata = Metadata().apply {
                    put(GrpcErrorMetadata.ERROR_CODE_KEY, ErrorCode.INTERNAL_ERROR.name)
                    put(GrpcErrorMetadata.ERROR_CODE_VALUE_KEY, ErrorCode.INTERNAL_ERROR.value.toString())
                }
                Status.INTERNAL
                    .withDescription("Internal error: ${throwable.message}")
                    .withCause(throwable)
                    .asException(metadata)
            }
        }
    }
}
