package application.cqrs.round

import domain.model.Round
import domain.vo.Amount

data class RoundItem(
    val round: Round,

    val totalPlace: Amount,

    val totalSettle: Amount,
)
