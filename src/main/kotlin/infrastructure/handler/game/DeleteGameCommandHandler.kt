package infrastructure.handler.game

import application.ICommandHandler
import application.command.game.DeleteGameCommand
import domain.repository.IGameRepository

class DeleteGameCommandHandler(
    private val gameRepository: IGameRepository,
) : ICommandHandler<DeleteGameCommand, Unit> {

    override suspend fun handle(command: DeleteGameCommand): Result<Unit> = runCatching {
        gameRepository.deleteByIdentity(command.identity)
    }
}
