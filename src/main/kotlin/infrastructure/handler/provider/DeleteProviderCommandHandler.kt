package infrastructure.handler.provider

import application.ICommandHandler
import application.command.provider.DeleteProviderCommand
import domain.repository.IProviderRepository

class DeleteProviderCommandHandler(
    private val providerRepository: IProviderRepository,
) : ICommandHandler<DeleteProviderCommand, Unit> {

    override suspend fun handle(command: DeleteProviderCommand): Result<Unit> = runCatching {
        providerRepository.deleteByIdentity(command.identity)
    }
}
