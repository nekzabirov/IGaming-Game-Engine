package application.command.collection

import application.ICommand
import domain.vo.Identity

/**
 * Remove a single game from a collection. Idempotent: if the game is not in
 * the collection the call is a no-op. The remaining games keep their
 * `sort_order` values (no compaction) — holes in the order sequence are
 * fine because the read side only uses it for `ORDER BY`.
 */
data class RemoveCollectionGameCommand(
    val identity: Identity,

    val gameIdentity: Identity,
) : ICommand<Unit>
