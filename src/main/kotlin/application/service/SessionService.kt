package application.service

import application.port.outbound.AggregatorAdapterRegistry
import application.port.outbound.EventPublisherAdapter
import application.port.outbound.PlayerLimitAdapter
import com.nekgamebling.application.port.outbound.storage.CacheAdapter
import domain.common.error.AggregatorNotSupportedError
import domain.common.error.NotFoundError
import domain.common.error.SessionInvalidError
import domain.common.error.ValidationError
import domain.common.event.SessionOpenedEvent
import domain.session.model.Session
import domain.session.repository.SessionRepository
import infrastructure.persistence.cache.CachingRepository
import shared.value.Currency
import shared.value.SessionToken
import domain.common.value.Locale
import domain.common.value.Platform
import shared.Logger
import java.security.SecureRandom
import java.util.Base64
import java.util.UUID
import kotlin.time.Duration.Companion.minutes

/**
 * Command for opening a new session.
 */
data class OpenSessionCommand(
    val gameIdentity: String,
    val playerId: String,
    val currency: Currency,
    val locale: Locale,
    val platform: Platform,
    val lobbyUrl: String,
    val spinMaxAmount: Long? = null
)

/**
 * Result of opening a session.
 */
data class OpenSessionResult(
    val session: Session,
    val launchUrl: String
)

/**
 * Application service for session-related operations.
 * Uses constructor injection for all dependencies.
 */
class SessionService(
    private val sessionRepository: SessionRepository,
    private val gameService: GameService,
    private val eventPublisher: EventPublisherAdapter,
    private val aggregatorRegistry: AggregatorAdapterRegistry,
    private val playerLimitAdapter: PlayerLimitAdapter,
    cacheAdapter: CacheAdapter
) {
    companion object {
        private val CACHE_TTL = 5.minutes
        private const val CACHE_PREFIX_TOKEN = "session:token:"
        private const val CACHE_PREFIX_ID = "session:id:"
    }

    private val secureRandom = SecureRandom()

    private val tokenCache = CachingRepository<Session>(
        cacheAdapter = cacheAdapter,
        cachePrefix = CACHE_PREFIX_TOKEN,
        ttl = CACHE_TTL
    )

    private val idCache = CachingRepository<Session>(
        cacheAdapter = cacheAdapter,
        cachePrefix = CACHE_PREFIX_ID,
        ttl = CACHE_TTL
    )

    /**
     * Generate a secure session token.
     */
    fun generateSessionToken(): String {
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    /**
     * Find session by token.
     */
    suspend fun findByToken(token: SessionToken): Result<Session> =
        tokenCache.getOrLoadResult(
            key = token.value,
            notFoundError = { SessionInvalidError(token.value) },
            loader = { sessionRepository.findByToken(token.value) }
        )

    /**
     * Find session by ID.
     */
    suspend fun findById(id: UUID): Result<Session> =
        idCache.getOrLoadResult(
            key = id.toString(),
            notFoundError = { NotFoundError("Session", id.toString()) },
            loader = { sessionRepository.findById(id) }
        )

    /**
     * Create a new session.
     */
    suspend fun createSession(session: Session): Result<Session> {
        val savedSession = sessionRepository.save(session)
        // Cache the new session
        tokenCache.update(savedSession.token, savedSession)
        idCache.update(savedSession.id.toString(), savedSession)
        return Result.success(savedSession)
    }

    /**
     * Invalidate cache for a session.
     */
    suspend fun invalidateCache(session: Session) {
        tokenCache.invalidate(session.token)
        idCache.invalidate(session.id.toString())
    }

    /**
     * Open a new game session.
     * Validates game support for locale/platform, creates session, and gets launch URL from aggregator.
     *
     * @param command The open session command
     * @return Result containing the session and launch URL
     */
    suspend fun open(command: OpenSessionCommand): Result<OpenSessionResult> {
        // Find game with details
        val game = gameService.findByIdentity(command.gameIdentity).getOrElse {
            Logger.error("[Session] open failed: game lookup error for identity=${command.gameIdentity}: ${it.message}")
            return Result.failure(it)
        }

        // Validate locale support
        if (!game.supportsLocale(command.locale)) {
            Logger.warn("[Session] open failed: unsupported locale=${command.locale.value} for game=${command.gameIdentity}")
            return Result.failure(
                ValidationError("locale", "Game does not support locale: ${command.locale.value}")
            )
        }

        // Validate platform support
        if (!game.supportsPlatform(command.platform)) {
            Logger.warn("[Session] open failed: unsupported platform=${command.platform} for game=${command.gameIdentity}")
            return Result.failure(
                ValidationError("platform", "Game does not support platform: ${command.platform}")
            )
        }

        // Get aggregator adapter
        val factory = aggregatorRegistry.getFactory(game.aggregator.aggregator)
            ?: run {
                Logger.error("[Session] open failed: aggregator not supported=${game.aggregator.aggregator.name}")
                return Result.failure(AggregatorNotSupportedError(game.aggregator.aggregator.name))
            }

        val launchUrlAdapter = factory.createLaunchUrlAdapter(game.aggregator)

        // Generate session token
        val token = generateSessionToken()

        // Create session
        val session = Session(
            id = UUID.randomUUID(),
            gameId = game.id,
            aggregatorId = game.aggregator.id,
            playerId = command.playerId,
            token = token,
            externalToken = null,
            currency = command.currency,
            locale = command.locale,
            platform = command.platform
        )

        // Save or clear spin limit
        if (command.spinMaxAmount != null) {
            playerLimitAdapter.saveSpinMax(command.playerId, command.spinMaxAmount)
        } else {
            playerLimitAdapter.deleteSpinMax(command.playerId)
        }

        // Save session
        val savedSession = createSession(session).getOrElse {
            return Result.failure(it)
        }

        // Get launch URL from aggregator
        val launchUrl = launchUrlAdapter.getLaunchUrl(
            gameSymbol = game.symbol,
            sessionToken = token,
            playerId = command.playerId,
            locale = command.locale,
            platform = command.platform,
            currency = command.currency,
            lobbyUrl = command.lobbyUrl,
            demo = false
        ).getOrElse {
            Logger.error("[Session] open failed: launch URL error for game=${game.symbol} player=${command.playerId}: ${it.message}")
            return Result.failure(it)
        }

        // Publish event
        eventPublisher.publish(
            SessionOpenedEvent(
                sessionId = savedSession.id.toString(),
                gameId = game.id.toString(),
                gameIdentity = game.identity,
                playerId = command.playerId,
                currency = command.currency,
                platform = command.platform.name
            )
        )

        return Result.success(OpenSessionResult(savedSession, launchUrl))
    }
}
