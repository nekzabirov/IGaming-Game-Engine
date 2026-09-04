package application.command.freespin

import application.ICommand

/** [id] is the grant id [infrastructure.gamehub.GameHubClient.createFreespin] returned — casino-engine
 *  keeps no local record to resolve a caller-chosen key against. */
data class CancelFreespinCommand(
    val id: String,
) : ICommand<Unit>
