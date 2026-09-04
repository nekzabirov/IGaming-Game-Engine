package infrastructure.handler.session

import application.ICommandHandler
import application.command.session.ReopenCasinoRoundSessionCommand
import domain.exception.domainRequireNotNull
import domain.exception.notfound.CasinoRoundNotFoundException
import domain.repository.ICasinoRoundRepository
import domain.vo.ExternalCasinoRoundId

class ReopenCasinoRoundSessionHandler(
    private val roundRepository: ICasinoRoundRepository,
) : ICommandHandler<ReopenCasinoRoundSessionCommand, Unit> {

    override suspend fun handle(command: ReopenCasinoRoundSessionCommand): Result<Unit> = runCatching {
        val round = domainRequireNotNull(
            roundRepository.findByExternalIdAndSessionId(
                externalId = ExternalCasinoRoundId(command.externalRoundId),
                sessionId = command.session.id,
            )
        ) { CasinoRoundNotFoundException() }

        // No event: closing already published CasinoRoundEvent, and a reopen followed by a second
        // close would tell crm a round finished twice. The correcting spin carries its own event.
        if (round.isFinished) {
            roundRepository.save(round.copy(finishedAt = null))
        }
    }
}
