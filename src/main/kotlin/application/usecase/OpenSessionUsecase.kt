package application.usecase

import application.port.external.IEventPort
import application.port.factory.IAggregatorFactory
import domain.event.SessionOpened
import domain.exception.DomainException
import domain.model.Session
import domain.repository.ISessionRepository
import org.slf4j.LoggerFactory

class OpenSessionUsecase(
    private val aggregatorFactory: IAggregatorFactory,
    private val sessionRepository: ISessionRepository,
    private val eventPort: IEventPort,
) {

    private val logger = LoggerFactory.getLogger(OpenSessionUsecase::class.java)

    suspend operator fun invoke(session: Session, lobbyUrl: String): Result<Response> = runCatching {
        val aggregator = session.gameVariant.game.provider.aggregator

        logger.info(
            "Opening session: player={} game={} aggregator={}",
            session.playerId.value, session.gameVariant.game.identity.value, aggregator.identity.value,
        )

        val gameAdapter = aggregatorFactory.createGameAdapter(aggregator)

        val launchUrl = gameAdapter.getLaunchUrl(session, lobbyUrl)

        val updatedSession = sessionRepository.save(session)

        eventPort.publish(SessionOpened(updatedSession))

        logger.info("Session opened: id={} player={}", updatedSession.id, updatedSession.playerId.value)

        Response(session = updatedSession, launchUrl = launchUrl)
    }.onFailure { e ->
        if (e !is DomainException) {
            logger.error(
                "Failed to open session: player={} game={}",
                session.playerId.value, session.gameVariant.game.identity.value, e,
            )
        }
    }

    data class Response(val session: Session, val launchUrl: String)
}
