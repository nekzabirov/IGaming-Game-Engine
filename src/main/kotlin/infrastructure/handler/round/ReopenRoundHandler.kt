package infrastructure.handler.round

import application.ICommandHandler
import application.command.round.ReopenRoundCommand
import domain.exception.domainRequireNotNull
import domain.exception.notfound.CasinoRoundNotFoundException
import domain.repository.ICasinoRoundRepository
import domain.vo.ExternalCasinoRoundId

class ReopenRoundHandler(
    private val roundRepository: ICasinoRoundRepository,
) : ICommandHandler<ReopenRoundCommand, Unit> {

    override suspend fun handle(command: ReopenRoundCommand): Result<Unit> = runCatching {
        val round = domainRequireNotNull(
            roundRepository.findByExternalId(ExternalCasinoRoundId(command.externalRoundId))
        ) { CasinoRoundNotFoundException() }

        // No event: closing already published CasinoRoundEvent, and a reopen followed by a second
        // close would tell crm a round finished twice. The correcting spin carries its own event.
        if (round.isFinished) {
            roundRepository.save(round.copy(finishedAt = null))
        }
    }
}
