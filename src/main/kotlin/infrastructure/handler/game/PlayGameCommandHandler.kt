package infrastructure.handler.game

import application.cqrs.ICommandHandler
import application.cqrs.game.PlayGameCommand
import application.port.external.IPlayerLimitPort
import application.port.storage.IGameVariantRepository
import application.usecase.OpenSessionUsecase
import domain.exception.domainRequireNotNull
import domain.exception.notfound.GameNotFoundException
import domain.model.Session
import domain.vo.Identity

class PlayGameCommandHandler(
    private val gameVariantRepository: IGameVariantRepository,
    private val playerLimitPort: IPlayerLimitPort,
    private val openSessionUsecase: OpenSessionUsecase,
) : ICommandHandler<PlayGameCommand, String> {

    companion object {
        private const val BASE24_CHARS = "BCDFGHJKMPQRTVWXY2346789"
        private const val TOKEN_LENGTH = 32
    }

    override suspend fun handle(command: PlayGameCommand): Result<String> = runCatching {
        val gameVariant = domainRequireNotNull(
            gameVariantRepository.findActiveByGameIdentity(command.identity)
        ) { GameNotFoundException() }

        if (command.maxSpinPlaceAmount != null) {
            playerLimitPort.saveMaxPlaceAmount(command.playerId, command.maxSpinPlaceAmount)
        }

        val session = Session(
            gameVariant = gameVariant,
            playerId = command.playerId,
            token = generateBase24Token(),
            externalToken = null,
            currency = command.currency,
            locale = command.locale,
            platform = command.platform,
        )

        val result = openSessionUsecase(session, lobbyUrl = "").getOrThrow()

        result.launchUrl
    }

    private fun generateBase24Token(): String = buildString(TOKEN_LENGTH) {
        repeat(TOKEN_LENGTH) {
            append(BASE24_CHARS.random())
        }
    }
}
