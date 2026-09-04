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
        // Null means a sportsbook leg — there is no per-game bonus-bet flag to gate it, so bonus
        // betting is allowed by default, the same as a freshly-synced CasinoGame.
        val bonusBetEnabled = spin.round.game?.bonusBetEnable ?: true

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
        // Affordability only gates PLACE: SETTLE/ROLLBACK credit the player and must
        // never be declined by the current balance (an all-in bet leaves balance 0,
        // which would block its own cashout).
        val canAfford = if (bonusBetEnabled) balance.canAfford(spin.amount)
                        else balance.canAffordWithReal(spin.amount)

        domainRequire(canAfford) { InsufficientBalanceException() }

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

    /**
     * A rollback moves money opposite to the spin it reverses: refunding a bet credits the player,
     * clawing back a win debits them. Both directions restore the original real/bonus split, so a
     * bonus-funded bet is refunded to the bonus pool rather than to real money.
     */
    private fun rollback(balance: PlayerBalance, spin: Spin): SpinResult {
        val reference = domainRequireNotNull(spin.reference) { SpinReferenceRequiredException() }

        return if (reference.isSettle) reclaim(balance, spin, reference)
               else refund(balance, spin, reference)
    }

    private fun refund(balance: PlayerBalance, spin: Spin, reference: Spin): SpinResult {
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

    private fun reclaim(balance: PlayerBalance, spin: Spin, reference: Spin): SpinResult {
        // Clamped to what is still there: a provider can give up on a transaction long after the
        // player has spent the win, and a balance cannot go negative. The spin records the amount
        // actually reclaimed, so any shortfall stays visible downstream instead of being implied.
        val fromReal = minOf(balance.realAmount, reference.realAmount)
        val fromBonus = minOf(balance.bonusAmount, reference.bonusAmount)

        val newBalance = balance.copy(
            realAmount = balance.realAmount - fromReal,
            bonusAmount = balance.bonusAmount - fromBonus
        )
        val processedSpin = spin.copy(
            amount = fromReal + fromBonus,
            realAmount = fromReal,
            bonusAmount = fromBonus
        )

        return SpinResult(newBalance, processedSpin)
    }
}
