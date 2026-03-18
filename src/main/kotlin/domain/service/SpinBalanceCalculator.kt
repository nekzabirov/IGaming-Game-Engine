package domain.service

import domain.exception.badrequest.SpinReferenceRequiredException
import domain.exception.forbidden.InsufficientBalanceException
import domain.exception.domainRequire
import domain.exception.domainRequireNotNull
import domain.model.PlayerBalance
import domain.model.Spin
import domain.model.SpinType
import domain.vo.Amount
import domain.vo.minOf

data class SpinResult(
    val balance: PlayerBalance,
    val spin: Spin
)

object SpinBalanceCalculator {
    fun process(balance: PlayerBalance, spin: Spin): SpinResult {
        val bonusBetEnabled = spin.round.gameVariant.game.bonusBetEnable

        val canAfford = if (bonusBetEnabled) balance.canAfford(spin.amount)
                        else balance.canAffordWithReal(spin.amount)

        domainRequire(canAfford) { InsufficientBalanceException() }

        return when (spin.type) {
            SpinType.PLACE    -> place(balance, spin, bonusBetEnabled)
            SpinType.SETTLE   -> settle(balance, spin)
            SpinType.ROLLBACK -> rollback(balance, spin)
        }
    }

    private fun place(
        balance: PlayerBalance,
        spin: Spin,
        bonusBetEnabled: Boolean
    ): SpinResult {
        val fromReal: Amount
        val fromBonus: Amount

        if (bonusBetEnabled) {
            fromReal = minOf(balance.realAmount, spin.amount)
            fromBonus = spin.amount - fromReal
        } else {
            fromReal = spin.amount
            fromBonus = Amount.ZERO
        }

        val newBalance = balance.copy(
            realAmount = balance.realAmount - fromReal,
            bonusAmount = balance.bonusAmount - fromBonus
        )
        val processedSpin = spin.copy(realAmount = fromReal, bonusAmount = fromBonus)

        return SpinResult(newBalance, processedSpin)
    }

    private fun settle(balance: PlayerBalance, spin: Spin): SpinResult {
        val usedBonus = (spin.reference?.bonusAmount ?: Amount.ZERO) > Amount.ZERO

        val newBalance = if (usedBonus)
            balance.copy(bonusAmount = balance.bonusAmount + spin.amount)
        else
            balance.copy(realAmount = balance.realAmount + spin.amount)

        val processedSpin = spin.copy(
            realAmount = if (usedBonus) Amount.ZERO else spin.amount,
            bonusAmount = if (usedBonus) spin.amount else Amount.ZERO
        )

        return SpinResult(newBalance, processedSpin)
    }

    private fun rollback(balance: PlayerBalance, spin: Spin): SpinResult {
        val reference = domainRequireNotNull(spin.reference) { SpinReferenceRequiredException() }

        val toReal = reference.realAmount
        val toBonus = reference.bonusAmount

        val newBalance = balance.copy(
            realAmount = balance.realAmount + toReal,
            bonusAmount = balance.bonusAmount + toBonus
        )
        val processedSpin = spin.copy(
            amount = toReal + toBonus,
            realAmount = toReal,
            bonusAmount = toBonus
        )

        return SpinResult(newBalance, processedSpin)
    }
}
