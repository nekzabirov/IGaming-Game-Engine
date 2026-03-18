package domain.service

import domain.exception.conflict.RoundAlreadyFinishedException
import domain.exception.domainRequire
import domain.model.Round
import domain.model.Spin
import domain.model.SpinType
import domain.vo.Amount

object SpinFactory {

    fun place(round: Round, externalId: String, amount: Amount): Spin {
        domainRequire(!round.isFinished) { RoundAlreadyFinishedException() }
        return Spin(
            externalId = externalId,
            round = round,
            type = SpinType.PLACE,
            amount = amount
        )
    }

    fun settle(round: Round, externalId: String, amount: Amount): Spin {
        domainRequire(!round.isFinished) { RoundAlreadyFinishedException() }
        return Spin(
            externalId = externalId,
            round = round,
            type = SpinType.SETTLE,
            amount = amount
        )
    }

    fun rollback(round: Round, externalId: String, amount: Amount): Spin =
        Spin(
            externalId = externalId,
            round = round,
            type = SpinType.ROLLBACK,
            amount = amount
        )
}
