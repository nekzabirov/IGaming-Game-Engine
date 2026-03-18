package application.cqrs.session

import application.cqrs.ICommand
import domain.model.PlayerBalance
import domain.vo.Amount

data class PlaceSpinSessionCommand(
    val sessionToken: String,
    val gameSymbol: String? = null,
    val externalRoundId: String,
    val externalSpinId: String,
    val freespinId: String? = null,
    val amount: Amount
) : ICommand<PlayerBalance>
