package domain.common.error

/**
 * Enumeration of all domain error codes.
 * Each error code has a unique numeric value for wire transmission
 * and a string representation for logging/debugging.
 *
 * Code ranges:
 * - 1xxx: General errors
 * - 2xxx: Balance/Betting errors
 * - 3xxx: Session errors
 * - 4xxx: Game errors
 * - 5xxx: Aggregator errors
 * - 6xxx: External service errors
 * - 9xxx: Internal errors
 */
enum class ErrorCode(val value: Int, val description: String) {
    // General errors (1xxx)
    NOT_FOUND(1000, "Entity not found"),
    VALIDATION_ERROR(1001, "Validation failed"),
    DUPLICATE_ENTITY(1002, "Entity already exists"),
    ILLEGAL_STATE(1003, "Operation not allowed in current state"),

    // Balance/Betting errors (2xxx)
    INSUFFICIENT_BALANCE(2000, "Insufficient balance"),
    SPIN_LIMIT_EXCEEDED(2001, "Spin limit exceeded"),

    // Session errors (3xxx)
    SESSION_INVALID(3000, "Session invalid or expired"),

    // Game errors (4xxx)
    GAME_UNAVAILABLE(4000, "Game unavailable"),
    ROUND_FINISHED(4001, "Round already finished"),
    ROUND_NOT_FOUND(4002, "Round not found"),
    INVALID_PRESET(4003, "Invalid preset configuration"),

    // Aggregator errors (5xxx)
    AGGREGATOR_NOT_SUPPORTED(5000, "Aggregator not supported"),
    AGGREGATOR_ERROR(5001, "Aggregator returned an error"),

    // External/Internal errors (6xxx, 9xxx)
    EXTERNAL_SERVICE_ERROR(6000, "External service error"),
    INTERNAL_ERROR(9999, "Internal server error");

    companion object {
        private val valueMap = entries.associateBy { it.value }
        private val nameMap = entries.associateBy { it.name }

        fun fromValue(value: Int): ErrorCode = valueMap[value] ?: INTERNAL_ERROR
        fun fromName(name: String): ErrorCode = nameMap[name] ?: INTERNAL_ERROR
    }
}
