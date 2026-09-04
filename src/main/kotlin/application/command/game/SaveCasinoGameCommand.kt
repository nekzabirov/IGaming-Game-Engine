package application.command.game

import application.ICommand
import domain.vo.Identity

/** Update-only — `name`/`tags`/`images`/every former-variant field and `rtp` are GameHub's, written
 *  only by the catalog sync. Fails with [domain.exception.notfound.CasinoGameNotFoundException]
 *  unless sync already created the game. */
data class SaveCasinoGameCommand(
    val identity: Identity,

    val bonusBetEnable: Boolean,
    val bonusWageringEnable: Boolean,

    val active: Boolean,

    val order: Int,
) : ICommand<Unit>
