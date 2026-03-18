package infrastructure.handler.freespin

import application.cqrs.ICommandHandler
import application.cqrs.freespin.CreateFreespinCommand
import application.port.factory.IAggregatoryFactory
import application.port.storage.IGameVariantRepository
import domain.exception.conflict.FreespinNotSupportedException
import domain.exception.domainRequire
import domain.exception.domainRequireNotNull
import domain.exception.notfound.GameNotFoundException

class CreateFreespinCommandHandler(
    private val gameVariantRepository: IGameVariantRepository,
    private val aggregatoryFactory: IAggregatoryFactory
) : ICommandHandler<CreateFreespinCommand, Unit> {

    override suspend fun handle(command: CreateFreespinCommand): Result<Unit> = runCatching {
        val variant = domainRequireNotNull(
            gameVariantRepository.findActiveByGameIdentity(command.gameIdentity)
        ) { GameNotFoundException() }

        domainRequire(variant.freeSpinEnable) { FreespinNotSupportedException() }

        val freespinAdapter = aggregatoryFactory.createFreespinAdapter(variant.game.provider.aggregator)

        freespinAdapter.create(
            presetValue = command.presetValues,
            referenceId = command.referenceId,
            playerId = command.playerId,
            gameSymbol = variant.symbol,
            currency = command.currency,
            startAt = command.startAt,
            endAt = command.endAt
        )
    }
}
