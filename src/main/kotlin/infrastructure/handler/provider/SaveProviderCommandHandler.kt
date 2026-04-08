package infrastructure.handler.provider

import application.ICommandHandler
import application.command.provider.SaveProviderCommand
import domain.repository.IAggregatorRepository
import domain.repository.IProviderRepository
import domain.exception.domainRequireNotNull
import domain.exception.notfound.AggregatorNotFoundException
import domain.model.Provider

class SaveProviderCommandHandler(
    private val providerRepository: IProviderRepository,
    private val aggregatorRepository: IAggregatorRepository,
) : ICommandHandler<SaveProviderCommand, Unit> {

    override suspend fun handle(command: SaveProviderCommand): Result<Unit> = runCatching {
        val aggregator = domainRequireNotNull(
            aggregatorRepository.findByIdentity(command.aggregatorIdentity)
        ) { AggregatorNotFoundException() }

        val existing = providerRepository.findByIdentity(command.identity)
        val provider = existing?.copy(
            name = command.name,
            order = command.order,
            active = command.active,
            aggregator = aggregator,
        ) ?: Provider(
            identity = command.identity,
            name = command.name,
            order = command.order,
            active = command.active,
            aggregator = aggregator,
        )

        providerRepository.save(provider)
    }
}
