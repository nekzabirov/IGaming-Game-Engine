package infrastructure.handler.provider

import application.ICommandHandler
import application.command.provider.SaveCasinoProviderCommand
import domain.repository.ICasinoProviderRepository
import domain.exception.domainRequireNotNull
import domain.exception.notfound.CasinoProviderNotFoundException

class SaveCasinoProviderCommandHandler(
    private val providerRepository: ICasinoProviderRepository,
) : ICommandHandler<SaveCasinoProviderCommand, Unit> {

    override suspend fun handle(command: SaveCasinoProviderCommand): Result<Unit> = runCatching {
        val existing = domainRequireNotNull(
            providerRepository.findByIdentity(command.identity)
        ) { CasinoProviderNotFoundException() }

        val provider = existing.copy(
            order = command.order,
            active = command.active,
            blockedCountry = command.blockedCountry,
        )

        providerRepository.save(provider)
    }
}
