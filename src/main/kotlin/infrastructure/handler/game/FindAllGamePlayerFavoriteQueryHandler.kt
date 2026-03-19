package infrastructure.handler.game

import application.cqrs.IQueryHandler
import application.cqrs.game.FindAllGamePlayerFavoriteQuery
import domain.model.Game
import domain.vo.Page
import infrastructure.persistence.entity.GameEntity
import infrastructure.persistence.entity.GameVariantEntity
import infrastructure.persistence.entity.ProviderEntity
import infrastructure.persistence.mapper.GameMapper.toDomain
import infrastructure.persistence.mapper.GameVariantMapper
import infrastructure.persistence.table.GameFavouriteTable
import infrastructure.persistence.table.GameVariantTable
import org.jetbrains.exposed.dao.with
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class FindAllGamePlayerFavoriteQueryHandler : IQueryHandler<FindAllGamePlayerFavoriteQuery, Page<Game>> {

    override suspend fun handle(query: FindAllGamePlayerFavoriteQuery): Page<Game> = newSuspendedTransaction {
        val totalItems = GameFavouriteTable
            .selectAll()
            .where { GameFavouriteTable.playerId eq query.playerId.value }
            .count()

        val pageable = query.pageable

        val gameIds = GameFavouriteTable
            .select(GameFavouriteTable.game)
            .where { GameFavouriteTable.playerId eq query.playerId.value }
            .orderBy(GameFavouriteTable.id to SortOrder.DESC)
            .limit(pageable.sizeReal)
            .offset(pageable.offset)
            .map { it[GameFavouriteTable.game] }

        val entities = GameEntity.forEntityIds(gameIds)
            .with(GameEntity::provider, GameEntity::collections, ProviderEntity::aggregator)
            .toList()

        val integrations = entities.map { it.provider.aggregator.integration }.distinct()

        val variantMap = GameVariantEntity.find {
            (GameVariantTable.game inList entities.map { it.id }) and
                    (GameVariantTable.integration inList integrations)
        }.toList().associateBy { it.game.id.value to it.integration }

        val gamesById = entities.associate { entity ->
            val game = entity.toDomain()
            val variantEntity = variantMap[entity.id.value to entity.provider.aggregator.integration]
            if (variantEntity != null) {
                game.variant = GameVariantMapper.run { variantEntity.toDomain() }
            }
            entity.id to game
        }

        val items = gameIds.mapNotNull { id -> gamesById[id] }

        Page(
            items = items,
            totalPages = pageable.getTotalPages(totalItems),
            totalItems = totalItems,
            currentPage = pageable.pageReal,
        )
    }
}
