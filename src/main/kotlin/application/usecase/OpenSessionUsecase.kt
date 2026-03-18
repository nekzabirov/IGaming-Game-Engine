package application.usecase

import application.event.SessionOpenEvent
import application.port.factory.IAggregatoryFactory
import application.port.external.IEventPort
import application.port.storage.ISessionRepository
import domain.model.Session

class OpenSessionUsecase(
    private val aggregatorFactory: IAggregatoryFactory,
    private val sessionRepository: ISessionRepository,
    private val eventAdapter: IEventPort,
) {

    suspend operator fun invoke(session: Session, lobbyUrl: String): Result<Response> = runCatching {
        val aggregator = session.gameVariant.game.provider.aggregator

        val gameAdapter = aggregatorFactory.createGameAdapter(aggregator)

        val launchUrl = gameAdapter.getLunchUrl(session, lobbyUrl)

        val updatedSession = sessionRepository.save(session)

        eventAdapter.publish(SessionOpenEvent(updatedSession))

        Response(session = updatedSession, launchUrl = launchUrl)
    }

    data class Response(val session: Session, val launchUrl: String)

}