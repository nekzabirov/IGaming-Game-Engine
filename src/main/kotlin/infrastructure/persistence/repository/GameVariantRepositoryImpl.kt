package infrastructure.persistence.repository

import application.port.storage.IGameVariantRepository
import domain.model.GameVariant
import domain.vo.Identity
import infrastructure.persistence.entity.GameEntity
import infrastructure.persistence.entity.GameVariantEntity
import infrastructure.persistence.entity.ProviderEntity
import infrastructure.persistence.mapper.GameVariantMapper.toDomain
import infrastructure.persistence.table.AggregatorTable
import infrastructure.persistence.table.GameTable
import infrastructure.persistence.table.GameVariantTable
import infrastructure.persistence.table.ProviderTable
import org.jetbrains.exposed.dao.load
import org.jetbrains.exposed.dao.with
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.batchUpsert
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update

class GameVariantRepositoryImpl : IGameVariantRepository {

    private val variantChain = arrayOf(
        GameVariantEntity::game,
        GameEntity::provider,
        GameEntity::collections,
        ProviderEntity::aggregator,
    )

    override suspend fun save(gameVariant: GameVariant): GameVariant = newSuspendedTransaction {
        if (gameVariant.id == Long.MIN_VALUE) {
            val id = GameVariantTable.insertAndGetId { it.fromDomain(gameVariant) }
            gameVariant.copy(id = id.value)
        } else {
            GameVariantTable.update({ GameVariantTable.id eq gameVariant.id }) { it.fromDomain(gameVariant) }
            gameVariant
        }
    }

    override suspend fun saveAll(gameVariants: List<GameVariant>): List<GameVariant> = newSuspendedTransaction {
        val gameIdentities = gameVariants.map { it.game.identity.value }.distinct()
        val gameIdMap = GameTable.select(GameTable.id, GameTable.identity)
            .where { GameTable.identity inList gameIdentities }
            .associate { it[GameTable.identity] to it[GameTable.id] }

        GameVariantTable.batchUpsert(gameVariants, keys = arrayOf(GameVariantTable.symbol)) { variant ->
            val gameId = gameIdMap[variant.game.identity.value]
                ?: error("Game not found: ${variant.game.identity.value}")

            this[GameVariantTable.symbol] = variant.symbol
            this[GameVariantTable.name] = variant.name
            this[GameVariantTable.integration] = variant.integration
            this[GameVariantTable.game] = gameId
            this[GameVariantTable.providerName] = variant.providerName
            this[GameVariantTable.freeSpinEnable] = variant.freeSpinEnable
            this[GameVariantTable.freeChipEnable] = variant.freeChipEnable
            this[GameVariantTable.jackpotEnable] = variant.jackpotEnable
            this[GameVariantTable.demoEnable] = variant.demoEnable
            this[GameVariantTable.bonusBuyEnable] = variant.bonusBuyEnable
            this[GameVariantTable.locales] = variant.locales.map { it.value }
            this[GameVariantTable.platforms] = variant.platforms.map { it.name }
            this[GameVariantTable.playLines] = variant.playLines
        }

        gameVariants
    }

    override suspend fun findById(id: Long): GameVariant? = newSuspendedTransaction {
        GameVariantEntity.findById(id)
            ?.load(*variantChain)
            ?.toDomain()
    }

    override suspend fun findBySymbol(symbol: String): GameVariant? = newSuspendedTransaction {
        GameVariantEntity.find { GameVariantTable.symbol eq symbol }
            .with(*variantChain)
            .firstOrNull()?.toDomain()
    }

    override suspend fun findAllByGame(gameIdentity: Identity): List<GameVariant> = newSuspendedTransaction {
        val gameId = GameTable.select(GameTable.id)
            .where { GameTable.identity eq gameIdentity.value }
            .singleOrNull()?.get(GameTable.id)
            ?: return@newSuspendedTransaction emptyList()

        GameVariantEntity.find { GameVariantTable.game eq gameId }
            .with(*variantChain)
            .toList()
            .map { it.toDomain() }
    }

    override suspend fun findAllByIntegration(integration: String): List<GameVariant> = newSuspendedTransaction {
        GameVariantEntity.find { GameVariantTable.integration eq integration }
            .with(*variantChain)
            .toList()
            .map { it.toDomain() }
    }

    override suspend fun findActiveByGameIdentity(gameIdentity: Identity): GameVariant? = newSuspendedTransaction {
        val variantId = GameVariantTable
            .join(GameTable, JoinType.INNER, GameVariantTable.game, GameTable.id)
            .join(ProviderTable, JoinType.INNER, GameTable.provider, ProviderTable.id)
            .join(AggregatorTable, JoinType.INNER, ProviderTable.aggregator, AggregatorTable.id)
            .select(GameVariantTable.id)
            .where {
                (GameTable.identity eq gameIdentity.value) and
                    (GameTable.active eq true) and
                    (ProviderTable.active eq true) and
                    (AggregatorTable.active eq true) and
                    (GameVariantTable.integration eq AggregatorTable.integration)
            }
            .firstOrNull()?.get(GameVariantTable.id)
            ?: return@newSuspendedTransaction null

        GameVariantEntity.findById(variantId)
            ?.load(*variantChain)
            ?.toDomain()
    }

    private fun UpdateBuilder<*>.fromDomain(gameVariant: GameVariant) {
        val gameId = GameTable.select(GameTable.id)
            .where { GameTable.identity eq gameVariant.game.identity.value }
            .singleOrNull()?.get(GameTable.id)
            ?: error("Game not found: ${gameVariant.game.identity.value}")

        this[GameVariantTable.symbol] = gameVariant.symbol
        this[GameVariantTable.name] = gameVariant.name
        this[GameVariantTable.integration] = gameVariant.integration
        this[GameVariantTable.game] = gameId
        this[GameVariantTable.providerName] = gameVariant.providerName
        this[GameVariantTable.freeSpinEnable] = gameVariant.freeSpinEnable
        this[GameVariantTable.freeChipEnable] = gameVariant.freeChipEnable
        this[GameVariantTable.jackpotEnable] = gameVariant.jackpotEnable
        this[GameVariantTable.demoEnable] = gameVariant.demoEnable
        this[GameVariantTable.bonusBuyEnable] = gameVariant.bonusBuyEnable
        this[GameVariantTable.locales] = gameVariant.locales.map { it.value }
        this[GameVariantTable.platforms] = gameVariant.platforms.map { it.name }
        this[GameVariantTable.playLines] = gameVariant.playLines
    }
}
