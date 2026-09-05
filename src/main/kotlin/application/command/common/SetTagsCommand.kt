package application.command.common

import application.ICommand
import domain.vo.Identity

/**
 * Polymorphic command for replacing the LOCAL tag list of a catalog entity.
 *
 * Deliberately writes `customTags`, never `tags`: the latter belongs to the catalog sync, which
 * overwrites it wholesale from the hub on every run — a tag written there would silently disappear
 * on the next pass. Concrete subclasses (`SetCasinoGameTagsCommand`, `SetCasinoProviderTagsCommand`)
 * only route to the right repository; a single [SetTagsCommandHandler] persists them.
 *
 * Replace, not append: the caller sends the list it wants to end up with, so removing a tag needs
 * no second RPC.
 */
interface SetTagsCommand : ICommand<Unit> {

    val identity: Identity

    val tags: List<String>
}
