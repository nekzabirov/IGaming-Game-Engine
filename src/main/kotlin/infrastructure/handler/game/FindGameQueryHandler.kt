package infrastructure.handler.game

import application.cqrs.IQueryHandler
import application.cqrs.game.FindGameQuery
import domain.model.Game
import infrastructure.persistence.entity.GameEntity
import infrastructure.persistence.entity.GameVariantEntity
import infrastructure.persistence.entity.ProviderEntity
import infrastructure.persistence.mapper.GameMapper.toDomain
import infrastructure.persistence.mapper.GameVariantMapper.toDomain
import infrastructure.persistence.table.GameTable
import infrastructure.persistence.table.GameVariantTable
import org.jetbrains.exposed.dao.with
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.Optional

class FindGameQueryHandler : IQueryHandler<FindGameQuery, Optional<Game>> {

    override suspend fun handle(query: FindGameQuery): Optional<Game> = newSuspendedTransaction {
        val entity = GameEntity.find { GameTable.identity eq query.identity.value }
            .with(GameEntity::provider, GameEntity::collections, ProviderEntity::aggregator)
            .firstOrNull() ?: return@newSuspendedTransaction Optional.empty()

        val game = entity.toDomain()

        val variantEntity = GameVariantEntity.find {
            (GameVariantTable.game eq entity.id) and
                    (GameVariantTable.integration eq entity.provider.aggregator.integration)
        }.firstOrNull()

        if (variantEntity != null) {
            game.variant = variantEntity.toDomain()
        }

        Optional.of(game)
    }
}
