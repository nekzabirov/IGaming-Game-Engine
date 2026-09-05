package infrastructure.handler.game

import application.query.game.CasinoGameFilter
import infrastructure.persistence.search.SearchIndexes
import infrastructure.persistence.search.searchCanRelax
import infrastructure.persistence.table.CollectionTable
import infrastructure.persistence.table.CasinoGameCollectionTable
import infrastructure.persistence.table.CasinoGameTable
import infrastructure.persistence.table.CasinoProviderTable
import org.jetbrains.exposed.sql.Expression
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.TextColumnType
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.castTo
import org.jetbrains.exposed.sql.exists
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.wrapAsExpression

private fun providerIsActive(): Op<Boolean> = exists(
    CasinoProviderTable
        .select(CasinoProviderTable.id)
        .where {
            (CasinoProviderTable.id eq CasinoGameTable.provider) and (CasinoProviderTable.active eq true)
        }
)

fun CasinoGameFilter.toCondition(relaxed: Boolean = false): Op<Boolean> {
    val conditions = buildList<Op<Boolean>> {
        add(providerIsActive())

        if (query.isNotBlank()) {
            add(SearchIndexes.games.matches(query, relaxed))
        }

        active?.let {
            add(Op.build { CasinoGameTable.active eq it })
        }

        bonusBetEnable?.let {
            add(Op.build { CasinoGameTable.bonusBetEnable eq it })
        }

        bonusWageringEnabled?.let {
            add(Op.build { CasinoGameTable.bonusWageringEnable eq it })
        }

        provider?.let { providerIdentity ->
            add(Op.build {
                CasinoGameTable.provider inSubQuery (
                    CasinoProviderTable
                        .select(CasinoProviderTable.id)
                        .where { CasinoProviderTable.identity eq providerIdentity.value }
                )
            })
        }

        collection?.let { collectionIdentity ->
            add(Op.build {
                CasinoGameTable.id inSubQuery (
                    (CasinoGameCollectionTable innerJoin CollectionTable)
                        .select(CasinoGameCollectionTable.game)
                        .where { CollectionTable.identity eq collectionIdentity.value }
                )
            })
        }

        freeSpinEnable?.let {
            add(Op.build { CasinoGameTable.freeSpinEnable eq it })
        }

        freeChipEnable?.let {
            add(Op.build { CasinoGameTable.freeChipEnable eq it })
        }

        jackpotEnable?.let {
            add(Op.build { CasinoGameTable.jackpotEnable eq it })
        }

        demoEnable?.let {
            add(Op.build { CasinoGameTable.demoEnable eq it })
        }

        bonusBuyEnable?.let {
            add(Op.build { CasinoGameTable.bonusBuyEnable eq it })
        }

        // Both tag lists are searched: `tags` is the hub's, `custom_tags` is ours, and a filter
        // has no business knowing which side a tag came from — the wire shows them merged anyway.
        if (inTags.isNotEmpty()) {
            add(inTags.map { tag ->
                Op.build { CasinoGameTable.tags.castTo<String>(TextColumnType()) like "%\"$tag\"%" } or
                    Op.build { CasinoGameTable.customTags.castTo<String>(TextColumnType()) like "%\"$tag\"%" }
            }.reduce { acc, op -> acc or op })
        }
    }

    return conditions.reduceOrNull { acc, op -> acc and op } ?: Op.TRUE
}

/**
 * Ordering that belongs with the filter: a collection-scoped listing IS a lobby rail,
 * so it follows the curated per-collection position instead of the catalog-wide one.
 * `game_collections` is keyed by (game, collection), so the correlated lookup resolves
 * to at most one row per game and needs no aggregate or DISTINCT.
 */
fun CasinoGameFilter.toOrdering(): Array<Pair<Expression<*>, SortOrder>> {
    // id tiebreaker: sortOrder is not unique (bulk-synced games share 0), and equal keys
    // give unstable pagination — a game could repeat or vanish across pages.
    val relevance = SearchIndexes.games.relevance(query)
    val leading = relevance?.leading ?: emptyArray()
    val trailing = relevance?.trailing ?: emptyArray()

    val collectionIdentity = collection
        ?: return leading +
            arrayOf<Pair<Expression<*>, SortOrder>>(CasinoGameTable.sortOrder to SortOrder.ASC) +
            trailing +
            arrayOf<Pair<Expression<*>, SortOrder>>(CasinoGameTable.id to SortOrder.ASC)

    val railPosition = wrapAsExpression<Int>(
        (CasinoGameCollectionTable innerJoin CollectionTable)
            .select(CasinoGameCollectionTable.sortOrder)
            .where {
                (CasinoGameCollectionTable.game eq CasinoGameTable.id) and
                    (CollectionTable.identity eq collectionIdentity.value)
            }
    )

    return leading +
        arrayOf<Pair<Expression<*>, SortOrder>>(railPosition to SortOrder.ASC) +
        trailing +
        arrayOf<Pair<Expression<*>, SortOrder>>(CasinoGameTable.id to SortOrder.ASC)
}

/** Both relevance tiers back to back — for the player's own lists, which carry no catalog order. */
fun CasinoGameFilter.relevanceOrdering(): Array<Pair<Expression<*>, SortOrder>> =
    SearchIndexes.games.relevanceOrdering(query)

/** Whether a failed search of this filter is worth retrying with the wide net. */
fun CasinoGameFilter.isRelaxable(): Boolean = query.isNotBlank() && searchCanRelax(query)
