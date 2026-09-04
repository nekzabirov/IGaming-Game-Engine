package application.command.freespin

import application.ICommand
import domain.vo.Currency
import domain.vo.Identity
import domain.vo.PlayerId

/** Proxies straight to the hub — [infrastructure.gamehub.GameHubClient.createFreespin] mints the
 *  grant id and returns it; casino-engine keeps no local record of the grant. There is no window
 *  (start/end) any more — the hub's contract carries none. */
data class CreateFreespinCommand(
    val gameIdentity: Identity,

    val playerId: PlayerId,

    val currency: Currency,

    val spinAmount: Long,

    val spinCount: Int,

    val presetValues: Map<String, Any>,
) : ICommand<String>
