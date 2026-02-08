package application.service

import application.port.outbound.AggregatorAdapterRegistry
import com.nekgamebling.application.port.outbound.storage.CacheAdapter
import domain.common.error.AggregatorNotSupportedError
import domain.common.error.GameUnavailableError
import domain.common.error.NotFoundError
import domain.common.error.ValidationError
import shared.Logger
import domain.game.model.Game
import domain.game.model.GameWithDetails
import domain.game.repository.GameRepository
import infrastructure.persistence.cache.CachingRepository
import shared.value.Currency
import domain.common.value.Aggregator
import domain.common.value.Locale
import domain.common.value.Platform
import java.util.UUID
import kotlin.time.Duration.Companion.minutes

/**
 * Result of demo game operation.
 */
data class DemoGameResult(
    val launchUrl: String
)

/**
 * Application service for game-related operations.
 * Uses constructor injection for all dependencies.
 */
class GameService(
    private val gameRepository: GameRepository,
    private val aggregatorRegistry: AggregatorAdapterRegistry,
    cacheAdapter: CacheAdapter
) {
    companion object {
        private val CACHE_TTL = 5.minutes
        private const val CACHE_PREFIX = "game:"
        private const val CACHE_PREFIX_SYMBOL = "game:symbol:"
    }

    private val detailsCache = CachingRepository<GameWithDetails>(
        cacheAdapter = cacheAdapter,
        cachePrefix = CACHE_PREFIX,
        ttl = CACHE_TTL
    )

    private val gameCache = CachingRepository<Game>(
        cacheAdapter = cacheAdapter,
        cachePrefix = CACHE_PREFIX_SYMBOL,
        ttl = CACHE_TTL
    )

    /**
     * Find game by identity with caching.
     */
    suspend fun findByIdentity(identity: String): Result<GameWithDetails> =
        detailsCache.getOrLoadResult(
            key = identity,
            notFoundError = { NotFoundError("Game", identity) },
            loader = { gameRepository.findWithDetailsByIdentity(identity) }
        )

    /**
     * Find game by ID.
     */
    suspend fun findById(id: UUID): Result<GameWithDetails> =
        detailsCache.getOrLoadResult(
            key = id.toString(),
            notFoundError = { NotFoundError("Game", id.toString()) },
            loader = { gameRepository.findWithDetailsById(id) }
        )

    /**
     * Find game by symbol with caching.
     */
    suspend fun findBySymbol(symbol: String, aggregator: Aggregator): Result<Game> =
        gameCache.getOrLoadResult(
            key = "$symbol:aggregator:$aggregator",
            notFoundError = { GameUnavailableError(symbol) },
            loader = { gameRepository.findBySymbol(symbol, aggregator) }
        )

    /**
     * Invalidate cache for a game.
     */
    suspend fun invalidateCache(identity: String) {
        detailsCache.invalidate(identity)
    }

    /**
     * Launch a game in demo mode.
     * Validates game support for locale/platform and gets demo launch URL from aggregator.
     *
     * @param gameIdentity The game identity
     * @param currency Currency for the demo session
     * @param locale Locale for the demo session
     * @param platform Platform for the demo session
     * @param lobbyUrl URL to return to after demo
     * @return Result containing the demo launch URL
     */
    suspend fun launchDemo(
        gameIdentity: String,
        currency: Currency,
        locale: Locale,
        platform: Platform,
        lobbyUrl: String
    ): Result<DemoGameResult> {
        val game = findByIdentity(gameIdentity).getOrElse {
            Logger.error("[GameService] launchDemo: game lookup failed for identity=$gameIdentity: ${it.message}")
            return Result.failure(it)
        }

        // Validate locale support
        if (!game.supportsLocale(locale)) {
            Logger.warn("[GameService] launchDemo: unsupported locale=${locale.value} for game=$gameIdentity")
            return Result.failure(
                ValidationError("locale", "Game does not support locale: ${locale.value}")
            )
        }

        // Validate platform support
        if (!game.supportsPlatform(platform)) {
            Logger.warn("[GameService] launchDemo: unsupported platform=$platform for game=$gameIdentity")
            return Result.failure(
                ValidationError("platform", "Game does not support platform: $platform")
            )
        }

        // Get aggregator adapter
        val factory = aggregatorRegistry.getFactory(game.aggregator.aggregator)
            ?: run {
                Logger.error("[GameService] launchDemo: aggregator not supported=${game.aggregator.aggregator.name}")
                return Result.failure(AggregatorNotSupportedError(game.aggregator.aggregator.name))
            }

        val launchUrlAdapter = factory.createLaunchUrlAdapter(game.aggregator)

        // Get demo launch URL
        val launchUrl = launchUrlAdapter.getLaunchUrl(
            gameSymbol = game.symbol,
            sessionToken = "demo",
            locale = locale,
            platform = platform,
            lobbyUrl = lobbyUrl,
            playerId = "demo",
            currency = currency,
            demo = true
        ).getOrElse {
            Logger.error("[GameService] launchDemo: launch URL error for game=${game.symbol}: ${it.message}")
            return Result.failure(it)
        }

        return Result.success(DemoGameResult(launchUrl))
    }
}
