package application.command.session

import application.ICommand
import domain.model.PlayerBalance
import domain.vo.Amount

data class SettleSpinSessionCommand(
    val sessionToken: String,

    val gameSymbol: String? = null,

    val externalRoundId: String,

    val externalSpinId: String,

    val freespinId: String? = null,

    val amount: Amount,
) : ICommand<PlayerBalance>
