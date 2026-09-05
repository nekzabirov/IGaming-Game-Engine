package domain.service

import domain.exception.conflict.CasinoRoundAlreadyFinishedException
import domain.exception.domainRequire
import domain.model.CasinoRound
import domain.model.Spin
import domain.model.SpinType
import domain.vo.Amount
import domain.vo.ExternalSpinId

object SpinFactory {

    fun place(round: CasinoRound, externalId: ExternalSpinId, amount: Amount): Spin {
        domainRequire(!round.isFinished) { CasinoRoundAlreadyFinishedException() }
        return Spin(
            externalId = externalId,
            round = round,
            type = SpinType.PLACE,
            amount = amount,
        )
    }

    /**
     * [reference] — ставка того же раунда, если она была оплачена бонусными деньгами.
     *
     * Именно по ней [SpinBalanceCalculator] решает, на какой счёт лечь выигрышу. Без неё выигрыш
     * бонусного раунда уходил на РЕАЛЬНЫЙ баланс, и бонус превращался в реальные деньги за один
     * спин, мимо всякого отыгрыша.
     */
    fun settle(
        round: CasinoRound,
        externalId: ExternalSpinId,
        amount: Amount,
        reference: Spin? = null,
    ): Spin {
        domainRequire(!round.isFinished) { CasinoRoundAlreadyFinishedException() }
        return Spin(
            externalId = externalId,
            round = round,
            reference = reference,
            type = SpinType.SETTLE,
            amount = amount,
        )
    }

    /**
     * A rollback always undoes a specific spin, so [reference] is required rather than optional —
     * [SpinBalanceCalculator] reads the original real/bonus split off it and cannot work without it.
     * A finished round is no obstacle: a provider may give up on a transaction long after the round
     * closed, and the money still has to move back.
     */
    fun rollback(round: CasinoRound, externalId: ExternalSpinId, reference: Spin): Spin =
        Spin(
            externalId = externalId,
            round = round,
            reference = reference,
            type = SpinType.ROLLBACK,
            amount = reference.amount,
        )
}
