package application.usecase

import application.port.external.IEventPublisherPort
import domain.event.CasinoRoundEvent
import domain.exception.DomainException
import domain.model.CasinoRound
import domain.repository.ICasinoRoundRepository
import org.slf4j.LoggerFactory

class FinishCasinoRoundUsecase(
    private val roundRepository: ICasinoRoundRepository,
    private val eventPublisher: IEventPublisherPort,
) {

    private val logger = LoggerFactory.getLogger(FinishCasinoRoundUsecase::class.java)

    suspend operator fun invoke(round: CasinoRound): Result<Unit> = runCatching {
        logger.info("Finishing round: id={}", round.id)

        val finishedRound = roundRepository.save(round.finish())

        // The round is committed — a broker failure must never fail the finish at this point.
        try {
            eventPublisher.publish(CasinoRoundEvent(finishedRound))
        } catch (e: Exception) {
            logger.error(
                "EVENT PUBLISH FAILED (event lost): route={} roundId={}",
                CasinoRoundEvent.route, finishedRound.id, e,
            )
        }

        logger.info("CasinoRound finished: id={}", finishedRound.id)
    }.onFailure { e ->
        if (e !is DomainException) {
            logger.error("Failed to finish round: id={}", round.id, e)
        }
    }
}
