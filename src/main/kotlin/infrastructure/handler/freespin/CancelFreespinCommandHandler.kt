package infrastructure.handler.freespin

import application.ICommandHandler
import application.command.freespin.CancelFreespinCommand
import application.port.factory.IAggregatorFactory
import domain.repository.IGameVariantRepository
import domain.exception.domainRequireNotNull
import domain.exception.notfound.GameNotFoundException

class CancelFreespinCommandHandler(
    private val gameVariantRepository: IGameVariantRepository,
    private val aggregatorFactory: IAggregatorFactory
) : ICommandHandler<CancelFreespinCommand, Unit> {

    override suspend fun handle(command: CancelFreespinCommand): Result<Unit> = runCatching {
        val variant = domainRequireNotNull(
            gameVariantRepository.findActiveByGameIdentity(command.gameIdentity)
        ) { GameNotFoundException() }

        val freespinAdapter = aggregatorFactory.createFreespinAdapter(variant.game.provider.aggregator)

        freespinAdapter.cancel(command.referenceId)
    }
}
