package infrastructure.handler.freespin

import application.ICommandHandler
import application.command.freespin.CreateFreespinCommand
import infrastructure.gamehub.GameHubClient

class CreateFreespinCommandHandler(
    private val gameHubClient: GameHubClient,
) : ICommandHandler<CreateFreespinCommand, Unit> {

    override suspend fun handle(command: CreateFreespinCommand): Result<Unit> = runCatching {
        gameHubClient.createFreespin(
            game = command.gameIdentity.value,
            playerId = command.playerId.value,
            amount = command.spinAmount,
            count = command.spinCount,
            currency = command.currency.value,
            presets = command.presetValues.mapValues { it.value.toString() },
            reference = command.referenceId,
            durationSeconds = command.duration.inWholeSeconds,
        )
    }
}
