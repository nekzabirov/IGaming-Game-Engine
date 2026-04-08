package infrastructure.handler.game

import infrastructure.persistence.entity.GameEntity
import infrastructure.persistence.entity.GameVariantEntity
import infrastructure.persistence.table.GameVariantTable
import org.jetbrains.exposed.sql.and

/**
 * Loads the active [GameVariantEntity] (if any) for each game in the receiver list,
 * matching variants to their game by `(game.id, provider.aggregator.integration)`.
 *
 * Used by every game-listing query handler so the lookup is defined once. Caller must
 * already be inside a `dbRead { }` / `dbTransaction { }` block.
 */
internal fun List<GameEntity>.loadVariantMap(): Map<Pair<Long, String>, GameVariantEntity> {
    if (isEmpty()) return emptyMap()
    val gameIds = map { it.id }
    val integrations = map { it.provider.aggregator.integration }.distinct()
    return GameVariantEntity.find {
        (GameVariantTable.game inList gameIds) and (GameVariantTable.integration inList integrations)
    }.associateBy { it.game.id.value to it.integration }
}

internal fun GameEntity.variantFrom(map: Map<Pair<Long, String>, GameVariantEntity>): GameVariantEntity? =
    map[id.value to provider.aggregator.integration]
