package infrastructure.handler.game

import application.IQueryHandler
import application.query.game.FindGameQuery
import application.query.game.GameView
import infrastructure.persistence.dbRead
import infrastructure.persistence.entity.GameEntity
import infrastructure.persistence.entity.GameVariantEntity
import infrastructure.persistence.entity.ProviderEntity
import infrastructure.persistence.mapper.GameMapper.toDomain
import infrastructure.persistence.mapper.GameVariantMapper.toDomain
import infrastructure.persistence.table.GameTable
import infrastructure.persistence.table.GameVariantTable
import org.jetbrains.exposed.dao.with
import org.jetbrains.exposed.sql.and
import java.util.Optional

class FindGameQueryHandler : IQueryHandler<FindGameQuery, Optional<GameView>> {

    override suspend fun handle(query: FindGameQuery): Optional<GameView> = dbRead {
        val entity = GameEntity.find { GameTable.identity eq query.identity.value }
            .with(GameEntity::provider, GameEntity::collections, ProviderEntity::aggregator)
            .firstOrNull() ?: return@dbRead Optional.empty()

        val variantEntity = GameVariantEntity.find {
            (GameVariantTable.game eq entity.id) and
                (GameVariantTable.integration eq entity.provider.aggregator.integration)
        }.firstOrNull()

        Optional.of(
            GameView(
                game = entity.toDomain(),
                variant = variantEntity?.toDomain(),
            )
        )
    }
}
