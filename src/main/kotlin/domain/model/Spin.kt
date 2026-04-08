package domain.model

import domain.vo.Amount
import domain.vo.ExternalSpinId

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

    val externalId: ExternalSpinId,

    val round: Round,

    val reference: Spin? = null,

    val type: SpinType,

    val amount: Amount,

    val realAmount: Amount = Amount(0),

    val bonusAmount: Amount = Amount(0),
) {
    val isPlace: Boolean
        get() = type == SpinType.PLACE

    val isSettle: Boolean
        get() = type == SpinType.SETTLE

    val isRollback: Boolean
        get() = type == SpinType.ROLLBACK
}
