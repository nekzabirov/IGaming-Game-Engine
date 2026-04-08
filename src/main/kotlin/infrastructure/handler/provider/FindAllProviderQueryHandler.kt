package infrastructure.handler.provider

import application.IQueryHandler
import application.query.provider.FindAllProviderQuery
import domain.model.Provider
import domain.vo.Page
import infrastructure.persistence.dbRead
import infrastructure.persistence.mapper.ProviderMapper.toProvider
import infrastructure.persistence.table.AggregatorTable
import infrastructure.persistence.table.CollectionTable
import infrastructure.persistence.table.GameCollectionTable
import infrastructure.persistence.table.GameTable
import infrastructure.persistence.table.ProviderTable
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.exists
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll

class FindAllProviderQueryHandler : IQueryHandler<FindAllProviderQuery, Page<Provider>> {

    override suspend fun handle(query: FindAllProviderQuery): Page<Provider> = dbRead {
        val filterCondition = buildFilterCondition(query)
        val pageable = query.pageable

        val totalItems = ProviderTable.selectAll().where { filterCondition }.count()

        val items = ProviderTable
            .join(AggregatorTable, JoinType.INNER, ProviderTable.aggregator, AggregatorTable.id)
            .selectAll()
            .where { filterCondition }
            .orderBy(ProviderTable.sortOrder)
            .limit(pageable.sizeReal)
            .offset(pageable.offset)
            .map { it.toProvider() }

        Page(
            items = items,
            totalPages = pageable.getTotalPages(totalItems),
            totalItems = totalItems,
            currentPage = pageable.pageReal,
        )
    }

    private fun buildFilterCondition(query: FindAllProviderQuery): Op<Boolean> {
        val conditions = buildList {
            if (query.query.isNotBlank()) {
                val pattern = "%${query.query.lowercase()}%"
                add(Op.build { (ProviderTable.identity like pattern) or (ProviderTable.name like pattern) })
            }
            query.active?.let { add(Op.build { ProviderTable.active eq it }) }
            query.aggregatorId?.let { aggId ->
                add(Op.build {
                    ProviderTable.aggregator inSubQuery (
                        AggregatorTable.select(AggregatorTable.id)
                            .where { AggregatorTable.identity eq aggId }
                    )
                })
            }

            if (query.inCollectionIdentities.isNotEmpty()) {
                add(exists(
                    GameTable
                        .join(GameCollectionTable, JoinType.INNER, GameTable.id, GameCollectionTable.game)
                        .select(GameTable.provider)
                        .where {
                            (GameTable.provider eq ProviderTable.id) and
                                    (GameCollectionTable.collection inSubQuery (
                                            CollectionTable
                                                .select(CollectionTable.id)
                                                .where { CollectionTable.identity inList query.inCollectionIdentities.map { it.value } }
                                            ))
                        }
                ))
            }
        }
        return conditions.reduceOrNull { acc, op -> acc and op } ?: Op.TRUE
    }
}
