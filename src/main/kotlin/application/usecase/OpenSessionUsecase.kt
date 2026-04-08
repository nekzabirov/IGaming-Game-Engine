package application.usecase

import application.port.external.IEventPort
import application.port.factory.IAggregatorFactory
import domain.event.SessionOpened
import domain.model.Session
import domain.repository.ISessionRepository

class OpenSessionUsecase(
    private val aggregatorFactory: IAggregatorFactory,
    private val sessionRepository: ISessionRepository,
    private val eventPort: IEventPort,
) {

    suspend operator fun invoke(session: Session, lobbyUrl: String): Result<Response> = runCatching {
        val aggregator = session.gameVariant.game.provider.aggregator

        val gameAdapter = aggregatorFactory.createGameAdapter(aggregator)

        val launchUrl = gameAdapter.getLaunchUrl(session, lobbyUrl)

        val updatedSession = sessionRepository.save(session)

        eventPort.publish(SessionOpened(updatedSession))

        Response(session = updatedSession, launchUrl = launchUrl)
    }

    data class Response(val session: Session, val launchUrl: String)
}
