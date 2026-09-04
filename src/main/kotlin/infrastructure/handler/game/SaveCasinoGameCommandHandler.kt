package infrastructure.handler.game

import application.ICommandHandler
import application.command.game.SaveCasinoGameCommand
import domain.repository.ICasinoGameRepository
import domain.exception.domainRequireNotNull
import domain.exception.notfound.CasinoGameNotFoundException

class SaveCasinoGameCommandHandler(
    private val gameRepository: ICasinoGameRepository,
) : ICommandHandler<SaveCasinoGameCommand, Unit> {

    override suspend fun handle(command: SaveCasinoGameCommand): Result<Unit> = runCatching {
        val existing = domainRequireNotNull(
            gameRepository.findByIdentity(command.identity)
        ) { CasinoGameNotFoundException() }

        val game = existing.copy(
            bonusBetEnable = command.bonusBetEnable,
            bonusWageringEnable = command.bonusWageringEnable,
            active = command.active,
            order = command.order,
        )

        gameRepository.save(game)
    }
}
