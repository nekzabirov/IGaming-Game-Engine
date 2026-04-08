package application.usecase

import application.port.external.IEventPort
import domain.model.Round
import domain.repository.IRoundRepository

class FinishRoundUsecase(
    private val roundRepository: IRoundRepository,
    private val eventPort: IEventPort,
) {

    suspend operator fun invoke(round: Round): Result<Unit> = runCatching {
        val (finishedRound, events) = round.finish()
        roundRepository.save(finishedRound)
        eventPort.publishAll(events)
    }
}
