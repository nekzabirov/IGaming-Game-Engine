package infrastructure.handler.sportbook

import application.ICommandHandler
import application.command.sportbook.OpenSportbookCommand
import application.command.sportbook.SportbookOpenResult
import infrastructure.gamehub.GameHubClient

class OpenSportbookHandler(
    private val gameHubClient: GameHubClient,
) : ICommandHandler<OpenSportbookCommand, SportbookOpenResult> {

    override suspend fun handle(command: OpenSportbookCommand): Result<SportbookOpenResult> = runCatching {
        val response = gameHubClient.openSportbook(
            playerId = command.playerId?.value,
            currency = command.currency.value,
            locale = command.locale.value,
        )

        SportbookOpenResult(integration = response.integration, data = response.dataMap)
    }
}
