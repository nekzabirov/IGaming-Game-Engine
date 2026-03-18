package infrastructure.handler.freespin

import application.cqrs.ICommandHandler
import application.cqrs.freespin.CancelFreespinCommand
import application.port.factory.IAggregatoryFactory
import application.port.storage.IGameVariantRepository
import domain.exception.domainRequireNotNull
import domain.exception.notfound.GameNotFoundException

class CancelFreespinCommandHandler(
    private val gameVariantRepository: IGameVariantRepository,
    private val aggregatoryFactory: IAggregatoryFactory
) : ICommandHandler<CancelFreespinCommand, Unit> {

    override suspend fun handle(command: CancelFreespinCommand): Result<Unit> = runCatching {
        val variant = domainRequireNotNull(
            gameVariantRepository.findActiveByGameIdentity(command.gameIdentity)
        ) { GameNotFoundException() }

        val freespinAdapter = aggregatoryFactory.createFreespinAdapter(variant.game.provider.aggregator)

        freespinAdapter.cancel(command.referenceId)
    }
}
