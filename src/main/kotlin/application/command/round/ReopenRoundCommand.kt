package application.command.round

import application.ICommand

/**
 * Reopens a round that a late movement has to land in.
 *
 * A rollback legitimately arrives after the round it corrects has closed — the hub sends the
 * compensating amount under a new leg id, minutes or hours later. [domain.service.SpinFactory]
 * refuses a place or a settle on a finished round, which is right for a vendor inventing new play
 * in a settled round and wrong for money that has to come back. A round that is already open is
 * left alone, so this is safe to call before any movement.
 */
data class ReopenRoundCommand(val externalRoundId: String) : ICommand<Unit>
