package infrastructure.handler.provider

import application.IQueryHandler
import application.query.provider.FindAllCasinoProviderQuery
import domain.model.CasinoProvider
import domain.vo.Page
import infrastructure.persistence.dbRead
import infrastructure.persistence.mapper.CasinoProviderMapper.toCasinoProvider
import infrastructure.persistence.search.SearchIndexes
import infrastructure.persistence.search.searchCanRelax
import infrastructure.persistence.search.searchPass
import infrastructure.persistence.table.CollectionTable
import infrastructure.persistence.table.CasinoGameCollectionTable
import infrastructure.persistence.table.CasinoGameTable
import infrastructure.persistence.table.CasinoProviderTable
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.TextColumnType
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.castTo
import org.jetbrains.exposed.sql.exists
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll

class FindAllCasinoProviderQueryHandler : IQueryHandler<FindAllCasinoProviderQuery, Page<CasinoProvider>> {

    override suspend fun handle(query: FindAllCasinoProviderQuery): Page<CasinoProvider> = dbRead {
        val pass = searchPass(
            relaxable = searchCanRelax(query.query),
            condition = { relaxed -> buildFilterCondition(query, relaxed) },
            count = { condition -> CasinoProviderTable.selectAll().where { condition }.count() },
        )
        val filterCondition = pass.condition
        val pageable = query.pageable

        val totalItems = pass.totalItems

        val items = CasinoProviderTable
            .selectAll()
            .where { filterCondition }
            .orderBy(
                *SearchIndexes.providers.relevanceOrdering(query.query),
                CasinoProviderTable.sortOrder to SortOrder.ASC,
            )
            .limit(pageable.sizeReal)
            .offset(pageable.offset)
            .map { it.toCasinoProvider() }

        Page(
            items = items,
            totalPages = pageable.getTotalPages(totalItems),
            totalItems = totalItems,
            currentPage = pageable.pageReal,
        )
    }

    private fun buildFilterCondition(query: FindAllCasinoProviderQuery, relaxed: Boolean): Op<Boolean> {
        val conditions = buildList {
            if (query.query.isNotBlank()) {
                add(SearchIndexes.providers.matches(query.query, relaxed))
            }
            query.active?.let { add(Op.build { CasinoProviderTable.active eq it }) }

            if (query.inCollectionIdentities.isNotEmpty()) {
                add(exists(
                    CasinoGameTable
                        .join(CasinoGameCollectionTable, JoinType.INNER, CasinoGameTable.id, CasinoGameCollectionTable.game)
                        .select(CasinoGameTable.provider)
                        .where {
                            (CasinoGameTable.provider eq CasinoProviderTable.id) and
                                    (CasinoGameCollectionTable.collection inSubQuery (
                                            CollectionTable
                                                .select(CollectionTable.id)
                                                .where { CollectionTable.identity inList query.inCollectionIdentities.map { it.value } }
                                            ))
                        }
                ))
            }

            if (query.inTags.isNotEmpty()) {
                add(query.inTags.map { tag ->
                    Op.build { CasinoProviderTable.tags.castTo<String>(TextColumnType()) like "%\"$tag\"%" }
                }.reduce { acc, op -> acc or op })
            }
        }
        return conditions.reduceOrNull { acc, op -> acc and op } ?: Op.TRUE
    }
}
