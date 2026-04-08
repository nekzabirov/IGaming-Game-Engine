package application.command.collection

import application.ICommand
import domain.vo.Identity

/**
 * Add a single game to a collection. Idempotent: if the game is already in
 * the collection the call is a no-op. On the happy path, the new membership
 * row lands at the end of the collection (`sort_order = max(existing) + 1`,
 * or `0` if the collection is empty).
 */
data class AddCollectionGameCommand(
    val identity: Identity,

    val gameIdentity: Identity,
) : ICommand<Unit>
