package application.command.session

import application.ICommand
import domain.model.CasinoSession

/**
 * Reopens a round that a late movement has to land in.
 *
 * Under the GameHub contract a rollback is not a distinguishable kind — it arrives as the
 * compensating amount under a new id, and it legitimately arrives after the round it corrects has
 * closed. `SpinFactory` refuses a place or a settle on a finished round, which is right for a
 * vendor inventing new play in a settled round and wrong for money that has to come back. Reopening
 * is the honest record: a round that money moved in afterwards was not finished when we said so.
 *
 * A round that is already open is left alone, so this is safe to call before any movement.
 */
data class ReopenCasinoRoundSessionCommand(
    val session: CasinoSession,

    val externalRoundId: String,
) : ICommand<Unit>
