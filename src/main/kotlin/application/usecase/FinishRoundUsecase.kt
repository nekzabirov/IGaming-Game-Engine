package application.usecase

import application.event.RoundEndEvent
import application.port.external.IEventPort
import application.port.storage.IRoundRepository
import domain.model.Round

class FinishRoundUsecase(
    private val roundRepository: IRoundRepository,
    private val eventPort: IEventPort
) {

    suspend operator fun invoke(round: Round): Result<Unit> = runCatching {
        val updatedRound = round.finish()
            .let { roundRepository.save(it) }

        eventPort.publish(RoundEndEvent(updatedRound))
    }

}