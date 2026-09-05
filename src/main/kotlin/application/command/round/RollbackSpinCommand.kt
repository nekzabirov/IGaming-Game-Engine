package application.command.round

import application.ICommand
import domain.model.PlayerBalance

/**
 * Reverses ONE already-committed leg, named by its own id — the hub's `RollbackSpinRequest` carries
 * nothing else, because it already knows exactly which leg it means. A leg that matches nothing
 * answers success anyway: the hub retries a rollback until it gets an answer, so "already gone" and
 * "never existed" both have to look like "done" to it.
 *
 * That "never existed" answer is a null balance. The request names no player, and without a leg
 * there is none to look one up by — the only honest answer is no balance at all.
 */
data class RollbackSpinCommand(val externalSpinId: String) : ICommand<PlayerBalance?>
