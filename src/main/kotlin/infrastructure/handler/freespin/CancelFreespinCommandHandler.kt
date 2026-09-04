package infrastructure.handler.freespin

import application.ICommandHandler
import application.command.freespin.CancelFreespinCommand
import infrastructure.gamehub.GameHubClient

class CancelFreespinCommandHandler(
    private val gameHubClient: GameHubClient,
) : ICommandHandler<CancelFreespinCommand, Unit> {

    override suspend fun handle(command: CancelFreespinCommand): Result<Unit> = runCatching {
        gameHubClient.cancelFreespin(command.id)
    }
}
