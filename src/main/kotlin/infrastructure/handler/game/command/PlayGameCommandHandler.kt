package com.nekgamebling.infrastructure.handler.game.command

import application.port.inbound.CommandHandler
import application.service.OpenSessionCommand
import application.service.SessionService
import com.nekgamebling.application.port.inbound.game.command.PlayGameCommand
import com.nekgamebling.application.port.inbound.game.command.PlayGameResponse

class PlayGameCommandHandler(
    private val sessionService: SessionService
) : CommandHandler<PlayGameCommand, PlayGameResponse> {

    override suspend fun handle(command: PlayGameCommand): Result<PlayGameResponse> {
        val openCommand = OpenSessionCommand(
            gameIdentity = command.identity,
            playerId = command.playerId,
            currency = command.currency,
            locale = command.locale,
            platform = command.platform,
            lobbyUrl = command.lobbyUrl,
            spinMaxAmount = command.spinMaxAmount
        )

        return sessionService.open(openCommand)
            .map { result ->
                PlayGameResponse(launchUrl = result.launchUrl)
            }
    }
}
