package services

import com.nekgamebling.game.v1.CasinoGameFilter
import db.CasinoGameCollections
import db.CasinoGames
import db.CasinoProviders
import db.Collections
import db.SearchIndexes
import db.searchCanRelax
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Expression
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.TextColumnType
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.castTo
import org.jetbrains.exposed.sql.exists
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.wrapAsExpression

// The proto filter is the query object itself — every game listing (catalog, favourites, rail,
// winners) turns it into one WHERE through here. `optional` booleans: unset = no filter.

private fun providerIsActive(): Op<Boolean> = exists(
    CasinoProviders
        .select(CasinoProviders.id)
        .where { (CasinoProviders.id eq CasinoGames.provider) and (CasinoProviders.active eq true) },
)

/** ANY-of match over a JSON array column, by text — the arrays are small and never indexed. */
fun Column<List<String>>.hasAnyTag(tags: List<String>): Op<Boolean> =
    tags.map { tag -> Op.build { castTo<String>(TextColumnType()) like "%\"$tag\"%" } }.reduce { acc, op -> acc or op }

fun CasinoGameFilter.toCondition(relaxed: Boolean = false): Op<Boolean> {
    val conditions = buildList<Op<Boolean>> {
        add(providerIsActive())

        if (query.isNotBlank()) add(SearchIndexes.games.matches(query, relaxed))

        if (hasActive()) add(Op.build { CasinoGames.active eq active })
        if (hasBonusBetEnable()) add(Op.build { CasinoGames.bonusBetEnable eq bonusBetEnable })
        if (hasBonusWageringEnable()) add(Op.build { CasinoGames.bonusWageringEnable eq bonusWageringEnable })

        if (hasProviderIdentity()) {
            add(Op.build {
                CasinoGames.provider inSubQuery CasinoProviders.select(CasinoProviders.id)
                    .where { CasinoProviders.identity eq providerIdentity }
            })
        }

        if (hasCollectionIdentity()) {
            add(Op.build {
                CasinoGames.id inSubQuery (CasinoGameCollections innerJoin Collections)
                    .select(CasinoGameCollections.game)
                    .where { Collections.identity eq collectionIdentity }
            })
        }

        if (hasFreeSpinEnable()) add(Op.build { CasinoGames.freeSpinEnable eq freeSpinEnable })
        if (hasFreeChipEnable()) add(Op.build { CasinoGames.freeChipEnable eq freeChipEnable })
        if (hasJackpotEnable()) add(Op.build { CasinoGames.jackpotEnable eq jackpotEnable })
        if (hasDemoEnable()) add(Op.build { CasinoGames.demoEnable eq demoEnable })
        if (hasBonusBuyEnable()) add(Op.build { CasinoGames.bonusBuyEnable eq bonusBuyEnable })

        // Both tag lists are searched: the hub's and ours — the wire shows them merged anyway.
        if (tagsList.isNotEmpty()) add(CasinoGames.tags.hasAnyTag(tagsList) or CasinoGames.customTags.hasAnyTag(tagsList))
    }

    return conditions.reduce { acc, op -> acc and op }
}

/**
 * A collection-scoped listing IS a lobby rail, so it follows the curated per-collection position
 * instead of the catalog-wide one. The id tiebreaker keeps pagination stable on equal keys.
 */
fun CasinoGameFilter.toOrdering(): Array<Pair<Expression<*>, SortOrder>> {
    val relevance = SearchIndexes.games.relevance(query)
    val leading = relevance?.leading ?: emptyArray()
    val trailing = relevance?.trailing ?: emptyArray()

    val position: Pair<Expression<*>, SortOrder> = if (hasCollectionIdentity()) {
        wrapAsExpression<Int>(
            (CasinoGameCollections innerJoin Collections)
                .select(CasinoGameCollections.sortOrder)
                .where { (CasinoGameCollections.game eq CasinoGames.id) and (Collections.identity eq collectionIdentity) },
        ) to SortOrder.ASC
    } else {
        CasinoGames.sortOrder to SortOrder.ASC
    }

    return leading + position + trailing + (CasinoGames.id to SortOrder.ASC)
}

/** Both relevance tiers back to back — for the player's own lists, which carry no catalog order. */
fun CasinoGameFilter.relevanceOrdering(): Array<Pair<Expression<*>, SortOrder>> =
    SearchIndexes.games.relevanceOrdering(query)

/** Whether a failed search of this filter is worth retrying with the wide net. */
fun CasinoGameFilter.isRelaxable(): Boolean = query.isNotBlank() && searchCanRelax(query)
