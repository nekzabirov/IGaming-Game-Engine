package infrastructure.handler.game

import application.ICommandHandler
import application.command.game.PlayCasinoGameCommand
import application.command.game.PlayCasinoGameResult
import application.port.external.IPlayerLimitPort
import application.usecase.OpenCasinoSessionUsecase
import domain.exception.domainRequireNotNull
import domain.exception.notfound.CasinoGameNotFoundException
import domain.repository.ICasinoGameRepository
import domain.service.CasinoSessionFactory

class PlayCasinoGameCommandHandler(
    private val gameRepository: ICasinoGameRepository,
    private val playerLimitPort: IPlayerLimitPort,
    private val openSessionUsecase: OpenCasinoSessionUsecase,
) : ICommandHandler<PlayCasinoGameCommand, PlayCasinoGameResult> {

    override suspend fun handle(command: PlayCasinoGameCommand): Result<PlayCasinoGameResult> = runCatching {
        val game = domainRequireNotNull(
            gameRepository.findByIdentity(command.identity)
        ) { CasinoGameNotFoundException() }

        if (command.maxSpinPlaceAmount != null) {
            playerLimitPort.saveMaxPlaceAmount(command.playerId, command.maxSpinPlaceAmount)
        }

        val session = CasinoSessionFactory.create(
            playerId = command.playerId,
            game = game,
            currency = command.currency,
            locale = command.locale,
            platform = command.platform,
        )

        val result = openSessionUsecase(session, lobbyUrl = "").getOrThrow()

        PlayCasinoGameResult(launchUrl = result.launchUrl)
    }
}
