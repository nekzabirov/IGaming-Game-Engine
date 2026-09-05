package infrastructure.handler.common

import application.ICommandHandler
import application.command.common.SetTagsCommand
import application.command.game.SetCasinoGameTagsCommand
import application.command.provider.SetCasinoProviderTagsCommand
import domain.repository.ICasinoGameRepository
import domain.repository.ICasinoProviderRepository

/**
 * Single entry point for every `SetXTagsCommand`.
 *
 * Mirrors [SetImageCommandHandler]: dispatches to the right repository's `setCustomTags(...)`,
 * which writes the LOCAL list only — the synced `tags` column stays the hub's.
 */
class SetTagsCommandHandler(
    private val gameRepository: ICasinoGameRepository,
    private val providerRepository: ICasinoProviderRepository,
) : ICommandHandler<SetTagsCommand, Unit> {

    override suspend fun handle(command: SetTagsCommand): Result<Unit> = runCatching {
        when (command) {
            is SetCasinoGameTagsCommand -> gameRepository.setCustomTags(command.identity, command.tags)
            is SetCasinoProviderTagsCommand -> providerRepository.setCustomTags(command.identity, command.tags)
            else -> error("Unhandled SetTagsCommand subtype: ${command::class.qualifiedName}")
        }
    }
}
