package domain.model

import domain.vo.Amount
import domain.vo.Currency

data class PlayerBalance(
    val realAmount: Amount,
    val bonusAmount: Amount,
    val currency: Currency,
) {
    val total: Amount get() = realAmount + bonusAmount

    fun canAfford(amount: Amount): Boolean = total >= amount

    fun canAffordWithReal(amount: Amount): Boolean = realAmount >= amount
}
