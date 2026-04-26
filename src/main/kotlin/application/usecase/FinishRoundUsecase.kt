package application.usecase

import application.port.external.IEventPort
import domain.exception.DomainException
import domain.model.Round
import domain.repository.IRoundRepository
import org.slf4j.LoggerFactory

class FinishRoundUsecase(
    private val roundRepository: IRoundRepository,
    private val eventPort: IEventPort,
) {

    private val logger = LoggerFactory.getLogger(FinishRoundUsecase::class.java)

    suspend operator fun invoke(round: Round): Result<Unit> = runCatching {
        logger.info("Finishing round: id={} session={}", round.id, round.session.id)

        val (finishedRound, events) = round.finish()
        roundRepository.save(finishedRound)
        eventPort.publishAll(events)

        logger.info("Round finished: id={}", finishedRound.id)
    }.onFailure { e ->
        if (e !is DomainException) {
            logger.error("Failed to finish round: id={} session={}", round.id, round.session.id, e)
        }
    }
}
