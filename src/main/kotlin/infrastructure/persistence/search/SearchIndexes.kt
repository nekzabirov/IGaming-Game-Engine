package infrastructure.persistence.search

import infrastructure.persistence.table.CasinoGameTable
import infrastructure.persistence.table.CasinoProviderTable
import infrastructure.persistence.table.CollectionTable
import org.jetbrains.exposed.sql.TextColumnType
import org.jetbrains.exposed.sql.castTo

/**
 * What each catalog listing searches over. Every entry here has a matching pair of expression
 * indexes in `V11__fuzzy_search.sql`; adding a column to one side without the other silently
 * turns that listing's search into a sequential scan.
 */
object SearchIndexes {

    val games = SearchIndex(CasinoGameTable.name, CasinoGameTable.identity)

    val providers = SearchIndex(
        CasinoProviderTable.name,
        CasinoProviderTable.identity,
    )

    val collections = SearchIndex(
        CollectionTable.name.castTo<String>(TextColumnType()),
        CollectionTable.identity,
    )
}
