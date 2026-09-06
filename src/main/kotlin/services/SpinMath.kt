package services

import clients.Balance
import errors.InsufficientBalanceException

/**
 * How a movement splits between the real and the bonus pool. Pure arithmetic over the balance
 * as the wallet reported it — what actually lands is always the wallet's own answer.
 */
object SpinMath {

    /** The amounts one leg moves: [amount] in total, [real] + [bonus] by pool. */
    data class Split(
        val amount: Long,
        val real: Long,
        val bonus: Long,
    )

    /**
     * A bet drains real first, then bonus — or real only when the game forbids bonus betting.
     * Affordability gates PLACE alone: SETTLE and ROLLBACK credit the player and are never declined
     * (an all-in bet leaves balance 0, which would block its own cashout).
     */
    fun place(balance: Balance, amount: Long, bonusBetEnabled: Boolean): Split {
        val affordable = if (bonusBetEnabled) balance.total >= amount else balance.real >= amount
        if (!affordable) throw InsufficientBalanceException()

        val real = if (bonusBetEnabled) minOf(balance.real, amount) else amount

        return Split(amount = amount, real = real, bonus = amount - real)
    }

    /** A win lands in the pool its bet came from: a bonus-funded round pays the bonus balance. */
    fun settle(amount: Long, referenceBonus: Long): Split =
        if (referenceBonus > 0) Split(amount, 0, amount) else Split(amount, amount, 0)

    /** Undoing a bet gives the exact original split back, pool for pool. */
    fun refund(referenceReal: Long, referenceBonus: Long): Split =
        Split(amount = referenceReal + referenceBonus, real = referenceReal, bonus = referenceBonus)

    /**
     * Undoing a win takes it back — clamped to what is still there, because a vendor can give up
     * on a transaction long after the player spent the win and a balance cannot go negative. The
     * spin records what was actually reclaimed, so any shortfall stays visible downstream.
     */
    fun reclaim(balance: Balance, referenceReal: Long, referenceBonus: Long): Split {
        val real = minOf(balance.real, referenceReal)
        val bonus = minOf(balance.bonus, referenceBonus)
        return Split(amount = real + bonus, real = real, bonus = bonus)
    }
}
