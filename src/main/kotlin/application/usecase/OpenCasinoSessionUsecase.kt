package application.usecase

import application.port.external.IEventPublisherPort
import domain.event.CasinoSessionEvent
import domain.exception.DomainException
import domain.model.CasinoSession
import infrastructure.gamehub.GameHubClient
import org.slf4j.LoggerFactory

/**
 * Launches through the hub directly — no adapter to resolve, no session to persist before the
 * call. [CasinoSession] exists only to be published on `session.events`; the hub owns the actual
 * launch/session bookkeeping on its side.
 */
class OpenCasinoSessionUsecase(
    private val gameHubClient: GameHubClient,
    private val eventPublisher: IEventPublisherPort,
) {

    private val logger = LoggerFactory.getLogger(OpenCasinoSessionUsecase::class.java)

    suspend operator fun invoke(
        session: CasinoSession,
        lobbyUrl: String,
        freespinId: String? = null,
    ): Result<Response> = runCatching {
        logger.info(
            "Opening session: player={} game={}",
            session.playerId.value, session.game.identity.value,
        )

        val launchUrl = gameHubClient.launchCasino(
            game = session.game.identity.value,
            playerId = session.playerId.value,
            currency = session.currency.value,
            locale = session.locale.value,
            lobbyUrl = lobbyUrl,
            platform = session.platform,
            freespinId = freespinId,
        )

        // The hub already accepted the launch — a broker failure past this point must never fail
        // the open.
        try {
            eventPublisher.publish(CasinoSessionEvent(session))
        } catch (e: Exception) {
            logger.error(
                "EVENT PUBLISH FAILED (event lost): route={} player={}",
                CasinoSessionEvent.route, session.playerId.value, e,
            )
        }

        logger.info("CasinoSession opened: player={} game={}", session.playerId.value, session.game.identity.value)

        Response(session = session, launchUrl = launchUrl)
    }.onFailure { e ->
        if (e !is DomainException) {
            logger.error(
                "Failed to open session: player={} game={}",
                session.playerId.value, session.game.identity.value, e,
            )
        }
    }

    data class Response(val session: CasinoSession, val launchUrl: String)
}
