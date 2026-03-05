package domain.common.error

/**
 * Base sealed class for all domain errors.
 * Using sealed class allows exhaustive when expressions and type-safe error handling.
 */
sealed class DomainError(
    override val message: String,
    override val cause: Throwable? = null
) : Exception(message, cause) {

    /** Type-safe error code enum */
    abstract val errorCode: ErrorCode

    /** String representation for backward compatibility and logging */
    val code: String get() = errorCode.name
}

/**
 * Entity was not found in the system.
 */
data class NotFoundError(
    val entity: String,
    val identifier: String,
    override val cause: Throwable? = null
) : DomainError("$entity not found: $identifier") {
    override val errorCode: ErrorCode = ErrorCode.NOT_FOUND
}

/**
 * Validation failed for the given input.
 */
data class ValidationError(
    val field: String,
    val reason: String,
    override val cause: Throwable? = null
) : DomainError("Validation failed for $field: $reason") {
    override val errorCode: ErrorCode = ErrorCode.VALIDATION_ERROR
}

/**
 * Player has insufficient balance for the operation.
 */
data class InsufficientBalanceError(
    val playerId: String,
    val required: Long,
    val available: Long,
    override val cause: Throwable? = null
) : DomainError("Insufficient balance for player $playerId: required $required, available $available") {
    override val errorCode: ErrorCode = ErrorCode.INSUFFICIENT_BALANCE
}

/**
 * Spin amount exceeds the configured limit.
 */
data class SpinLimitExceededError(
    val playerId: String,
    val spinAmount: Long,
    val limit: Long,
    override val cause: Throwable? = null
) : DomainError("Spin limit exceeded for player $playerId: spin $spinAmount, limit $limit") {
    override val errorCode: ErrorCode = ErrorCode.SPIN_LIMIT_EXCEEDED
}

/**
 * Session is not valid or has expired.
 */
data class SessionInvalidError(
    val sessionToken: String,
    val reason: String = "Session is invalid or expired",
    override val cause: Throwable? = null
) : DomainError("$reason: $sessionToken") {
    override val errorCode: ErrorCode = ErrorCode.SESSION_INVALID
}

/**
 * Game is not available for play.
 */
data class GameUnavailableError(
    val gameIdentity: String,
    val reason: String = "Game is not available",
    override val cause: Throwable? = null
) : DomainError("$reason: $gameIdentity") {
    override val errorCode: ErrorCode = ErrorCode.GAME_UNAVAILABLE
}

/**
 * Round has already been finished and cannot be modified.
 */
data class RoundFinishedError(
    val roundId: String,
    override val cause: Throwable? = null
) : DomainError("Round already finished: $roundId") {
    override val errorCode: ErrorCode = ErrorCode.ROUND_FINISHED
}

/**
 * Round was not found.
 */
data class RoundNotFoundError(
    val roundId: String,
    override val cause: Throwable? = null
) : DomainError("Round not found: $roundId") {
    override val errorCode: ErrorCode = ErrorCode.ROUND_NOT_FOUND
}

/**
 * Preset configuration is invalid.
 */
data class InvalidPresetError(
    val presetId: String,
    val reason: String,
    override val cause: Throwable? = null
) : DomainError("Invalid preset $presetId: $reason") {
    override val errorCode: ErrorCode = ErrorCode.INVALID_PRESET
}

/**
 * External service (aggregator, wallet, etc.) returned an error.
 */
data class ExternalServiceError(
    val service: String,
    val details: String,
    override val cause: Throwable? = null
) : DomainError("External service error from $service: $details") {
    override val errorCode: ErrorCode = ErrorCode.EXTERNAL_SERVICE_ERROR
}

/**
 * Operation is not allowed in the current state.
 */
data class IllegalStateError(
    val operation: String,
    val currentState: String,
    override val cause: Throwable? = null
) : DomainError("Operation '$operation' not allowed in state: $currentState") {
    override val errorCode: ErrorCode = ErrorCode.ILLEGAL_STATE
}

/**
 * Duplicate entity error.
 */
data class DuplicateEntityError(
    val entity: String,
    val identifier: String,
    override val cause: Throwable? = null
) : DomainError("$entity already exists: $identifier") {
    override val errorCode: ErrorCode = ErrorCode.DUPLICATE_ENTITY
}

/**
 * Aggregator is not supported.
 */
data class AggregatorNotSupportedError(
    val aggregator: String,
    override val cause: Throwable? = null
) : DomainError("Aggregator not supported: $aggregator") {
    override val errorCode: ErrorCode = ErrorCode.AGGREGATOR_NOT_SUPPORTED
}
