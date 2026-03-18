package infrastructure.persistence.repository

import application.port.storage.IGameRepository
import domain.model.Game
import domain.vo.Identity
import domain.vo.Page
import domain.vo.Pageable
import infrastructure.persistence.entity.CollectionEntity
import infrastructure.persistence.entity.GameEntity
import infrastructure.persistence.entity.ProviderEntity
import infrastructure.persistence.mapper.GameMapper.toDomain
import infrastructure.persistence.table.CollectionTable
import infrastructure.persistence.table.GameTable
import infrastructure.persistence.table.ProviderTable
import org.jetbrains.exposed.dao.with
import org.jetbrains.exposed.sql.SizedCollection
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.batchUpsert
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class GameRepositoryImpl : IGameRepository {

    private val gameChain = arrayOf(
        GameEntity::provider,
        GameEntity::collections,
        ProviderEntity::aggregator,
    )

    override suspend fun save(game: Game): Game = newSuspendedTransaction {
        val providerEntity = ProviderEntity.find { ProviderTable.identity eq game.provider.identity.value }.single()

        val gameEntity = GameEntity.find { GameTable.identity eq game.identity.value }.firstOrNull()

        val entity = if (gameEntity != null) {
            gameEntity.apply {
                name = game.name
                provider = providerEntity
                bonusBetEnable = game.bonusBetEnable
                bonusWageringEnable = game.bonusWageringEnable
                tags = game.tags
                active = game.active
                images = game.images.data
                sortOrder = game.order
            }
        } else {
            GameEntity.new {
                identity = game.identity.value
                name = game.name
                provider = providerEntity
                bonusBetEnable = game.bonusBetEnable
                bonusWageringEnable = game.bonusWageringEnable
                tags = game.tags
                active = game.active
                images = game.images.data
                sortOrder = game.order
            }
        }

        val collectionIdentities = game.collections.map { it.identity.value }
        val collectionEntities = CollectionEntity.find {
            CollectionTable.identity inList collectionIdentities
        }.toList()
        entity.collections = SizedCollection(collectionEntities)

        game
    }

    override suspend fun saveAll(gameList: List<Game>): List<Game> = newSuspendedTransaction {
        val providerIdentities = gameList.map { it.provider.identity.value }.distinct()
        val providerMap = ProviderEntity.find { ProviderTable.identity inList providerIdentities }
            .associateBy { it.identity }

        GameTable.batchUpsert(gameList, keys = arrayOf(GameTable.identity)) { game ->
            val providerEntity = providerMap[game.provider.identity.value]
                ?: error("Provider not found: ${game.provider.identity.value}")

            this[GameTable.identity] = game.identity.value
            this[GameTable.name] = game.name
            this[GameTable.provider] = providerEntity.id
            this[GameTable.bonusBetEnable] = game.bonusBetEnable
            this[GameTable.bonusWageringEnable] = game.bonusWageringEnable
            this[GameTable.tags] = game.tags
            this[GameTable.active] = game.active
            this[GameTable.images] = game.images.data
            this[GameTable.sortOrder] = game.order
        }

        gameList
    }

    override suspend fun findByIdentity(identity: Identity): Game? = newSuspendedTransaction {
        GameEntity.find { GameTable.identity eq identity.value }
            .with(*gameChain)
            .firstOrNull()?.toDomain()
    }

    override suspend fun findAll(): List<Game> = newSuspendedTransaction {
        GameEntity.all()
            .with(*gameChain)
            .toList()
            .map { it.toDomain() }
    }

    override suspend fun findAll(pageable: Pageable): Page<Game> = newSuspendedTransaction {
        val totalItems = GameEntity.count()

        val items = GameEntity.all()
            .orderBy(GameTable.sortOrder to SortOrder.ASC)
            .limit(pageable.sizeReal)
            .offset(pageable.offset)
            .with(*gameChain)
            .toList()
            .map { it.toDomain() }

        Page(
            items = items,
            totalPages = pageable.getTotalPages(totalItems),
            totalItems = totalItems,
            currentPage = pageable.pageReal,
        )
    }
}
