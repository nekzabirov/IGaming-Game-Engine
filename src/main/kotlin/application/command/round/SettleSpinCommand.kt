package application.command.round

import application.ICommand
import domain.model.PlayerBalance
import domain.vo.Amount
import domain.vo.Currency
import domain.vo.Identity
import domain.vo.PlayerId

/**
 * [gameIdentity] null means a sportsbook leg. A settle can legitimately be the first thing heard
 * about a round — a win-only call for a bonus payout — so the handler opens the round exactly as
 * [PlaceSpinCommand] does, it just usually does not need to.
 */
data class SettleSpinCommand(
    val externalSpinId: String,

    val externalRoundId: String,

    val playerId: PlayerId,

    val gameIdentity: Identity?,

    val amount: Amount,

    val currency: Currency,

    val freespinId: String? = null,
) : ICommand<PlayerBalance>
