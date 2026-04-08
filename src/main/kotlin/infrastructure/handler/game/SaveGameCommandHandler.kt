package infrastructure.handler.game

import application.ICommandHandler
import application.command.game.SaveGameCommand
import domain.repository.IGameRepository
import domain.repository.IProviderRepository
import domain.exception.domainRequireNotNull
import domain.exception.notfound.ProviderNotFoundException
import domain.model.Game

class SaveGameCommandHandler(
    private val gameRepository: IGameRepository,
    private val providerRepository: IProviderRepository,
) : ICommandHandler<SaveGameCommand, Unit> {

    override suspend fun handle(command: SaveGameCommand): Result<Unit> = runCatching {
        val provider = domainRequireNotNull(
            providerRepository.findByIdentity(command.providerIdentity)
        ) { ProviderNotFoundException() }

        val existing = gameRepository.findByIdentity(command.identity)
        val game = existing?.copy(
            name = command.name,
            provider = provider,
            bonusBetEnable = command.bonusBetEnable,
            bonusWageringEnable = command.bonusWageringEnable,
            tags = command.tags,
        ) ?: Game(
            identity = command.identity,
            name = command.name,
            provider = provider,
            bonusBetEnable = command.bonusBetEnable,
            bonusWageringEnable = command.bonusWageringEnable,
            tags = command.tags,
        )

        gameRepository.save(game)
    }
}
