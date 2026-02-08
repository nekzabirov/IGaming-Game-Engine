package application.service

import application.port.outbound.AggregatorAdapterRegistry
import application.port.outbound.storage.AggregatorInfoRepository
import application.port.outbound.GameSyncAdapter
import application.port.outbound.GameVariantRepository
import domain.common.error.AggregatorNotSupportedError
import domain.common.error.NotFoundError
import domain.game.model.GameVariant
import java.util.UUID

/**
 * Result of game sync operation.
 */
data class SyncGameResult(
    val gameCount: Int,
    val providerCount: Int
)

/**
 * Application service for syncing games from aggregators.
 * Handles the orchestration of fetching games from aggregator APIs and saving them locally.
 */
class GameSyncService(
    private val aggregatorRegistry: AggregatorAdapterRegistry,
    private val gameSyncAdapter: GameSyncAdapter,
    private val aggregatorInfoRepository: AggregatorInfoRepository,
    private val gameVariantRepository: GameVariantRepository
) {

    /**
     * Sync games from an aggregator.
     *
     * @param aggregatorIdentity The identity of the aggregator to sync from
     * @return Result containing the count of synced games and providers
     */
    suspend fun sync(aggregatorIdentity: String): Result<SyncGameResult> {
        val aggregatorInfo = aggregatorInfoRepository.findByIdentity(aggregatorIdentity)
            ?: return Result.failure(NotFoundError("Aggregator", aggregatorIdentity))

        val factory = aggregatorRegistry.getFactory(aggregatorInfo.aggregator)
            ?: return Result.failure(AggregatorNotSupportedError(aggregatorInfo.aggregator.name))

        val aggregatorAdapter = factory.createGameSyncAdapter(aggregatorInfo)

        val games = aggregatorAdapter.listGames().getOrElse {
            return Result.failure(it)
        }

        val variants = games
            .map { aggregatorGame ->
                GameVariant(
                    id = UUID.randomUUID(),
                    symbol = aggregatorGame.symbol,
                    name = aggregatorGame.name,
                    providerName = aggregatorGame.providerName,
                    aggregator = aggregatorInfo.aggregator,
                    freeSpinEnable = aggregatorGame.freeSpinEnable,
                    freeChipEnable = aggregatorGame.freeChipEnable,
                    jackpotEnable = aggregatorGame.jackpotEnable,
                    demoEnable = aggregatorGame.demoEnable,
                    bonusBuyEnable = aggregatorGame.bonusBuyEnable,
                    locales = aggregatorGame.locales,
                    platforms = aggregatorGame.platforms,
                    playLines = aggregatorGame.playLines
                )
            }
            .let { variantsList ->
                gameVariantRepository.saveAll(variantsList)
            }

        gameSyncAdapter.syncGame(variants, aggregatorInfo)

        val gameCount = variants.size
        val providerNames = variants.map { it.providerName }.distinct()

        return Result.success(SyncGameResult(gameCount, providerNames.size))
    }
}
