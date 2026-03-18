package domain.model

import domain.vo.Amount

/**
 * Spin type representing the state of a spin transaction.
 */
enum class SpinType {
    PLACE,
    SETTLE,
    ROLLBACK
}

data class Spin(
    val id: Long = Long.MIN_VALUE,

    val externalId: String,

    val round: Round,

    val reference: Spin? = null,

    val type: SpinType,

    val amount: Amount,

    val realAmount: Amount = Amount(0),

    val bonusAmount: Amount = Amount(0),
)
