package application.command.round

import application.ICommand
import domain.model.PlayerBalance
import domain.vo.Amount
import domain.vo.Currency
import domain.vo.Identity
import domain.vo.PlayerId

/** [gameIdentity] null means a sportsbook leg — the hub sends an empty `game` for those. */
data class PlaceSpinCommand(
    val externalSpinId: String,

    val externalRoundId: String,

    val playerId: PlayerId,

    val gameIdentity: Identity?,

    val amount: Amount,

    val currency: Currency,

    val freespinId: String? = null,
) : ICommand<PlayerBalance>
