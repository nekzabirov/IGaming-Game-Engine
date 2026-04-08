package application.command.collection

import application.ICommand
import domain.vo.Identity

/**
 * Set the per-collection sort order of a single game inside a collection.
 *
 * Fails with `CollectionNotFoundException` if the collection does not exist,
 * or `GameNotFoundException` if the game is not currently a member of the
 * collection. The caller is responsible for avoiding collisions — the read
 * side breaks ties on `GameTable.id`.
 */
data class UpdateCollectionGameOrderCommand(
    val identity: Identity,

    val gameIdentity: Identity,

    val order: Int,
) : ICommand<Unit>
