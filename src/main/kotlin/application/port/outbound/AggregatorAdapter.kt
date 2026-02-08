package application.port.outbound

import domain.aggregator.AggregatorGame
import domain.aggregator.AggregatorInfo
import domain.common.value.Aggregator
import shared.value.Currency
import domain.common.value.Locale
import domain.common.value.Platform
import kotlinx.datetime.LocalDateTime

/**
 * Port for getting game launch URLs from aggregators.
 */
interface AggregatorLaunchUrlPort {
    /**
     * Get launch URL for a game.
     */
    suspend fun getLaunchUrl(
        gameSymbol: String,
        sessionToken: String,
        playerId: String,
        locale: Locale,
        platform: Platform,
        currency: Currency,
        lobbyUrl: String,
        demo: Boolean
    ): Result<String>
}

/**
 * Port for managing freespins with aggregators.
 */
interface AggregatorFreespinPort {
    /**
     * Get freespin preset configuration for a game.
     */
    suspend fun getPreset(gameSymbol: String): Result<Map<String, Any>>

    /**
     * Create a freespin for a player.
     */
    suspend fun createFreespin(
        presetValue: Map<String, Int>,
        referenceId: String,
        playerId: String,
        gameSymbol: String,
        currency: Currency,
        startAt: LocalDateTime,
        endAt: LocalDateTime
    ): Result<Unit>

    /**
     * Cancel an existing freespin.
     */
    suspend fun cancelFreespin(
        referenceId: String,
    ): Result<Unit>
}

/**
 * Port for syncing games from aggregators.
 */
interface AggregatorGameSyncPort {
    /**
     * List all games available from an aggregator.
     */
    suspend fun listGames(): Result<List<AggregatorGame>>
}

/**
 * Factory for creating aggregator-specific adapters.
 * Implementations should return the appropriate adapter based on aggregator type.
 */
interface AggregatorAdapterFactory {
    /**
     * Check if this factory supports the given aggregator type.
     */
    fun supports(aggregator: Aggregator): Boolean

    /**
     * Create launch URL adapter for the aggregator.
     */
    fun createLaunchUrlAdapter(aggregatorInfo: AggregatorInfo): AggregatorLaunchUrlPort

    /**
     * Create freespin adapter for the aggregator.
     */
    fun createFreespinAdapter(aggregatorInfo: AggregatorInfo): AggregatorFreespinPort

    /**
     * Create game sync adapter for the aggregator.
     */
    fun createGameSyncAdapter(aggregatorInfo: AggregatorInfo): AggregatorGameSyncPort
}

/**
 * Registry for managing multiple aggregator adapter factories.
 */
interface AggregatorAdapterRegistry {
    /**
     * Get the factory for the given aggregator type.
     */
    fun getFactory(aggregator: Aggregator): AggregatorAdapterFactory?
}
