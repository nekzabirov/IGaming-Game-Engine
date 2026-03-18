package infrastructure.handler.provider

import application.cqrs.IQueryHandler
import application.cqrs.provider.FindAllProviderQuery
import application.cqrs.provider.ProviderItem
import domain.vo.Page
import infrastructure.persistence.mapper.ProviderMapper.toProvider
import infrastructure.persistence.table.AggregatorTable
import infrastructure.persistence.table.GameTable
import infrastructure.persistence.table.ProviderTable
import org.jetbrains.exposed.sql.Case
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.LongColumnType
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Sum
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.longLiteral
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class FindAllProviderQueryHandler : IQueryHandler<FindAllProviderQuery, Page<ProviderItem>> {

    override suspend fun handle(query: FindAllProviderQuery): Page<ProviderItem> = newSuspendedTransaction {
        val filterCondition = buildFilterCondition(query)

        val totalItems = ProviderTable.selectAll().where { filterCondition }.count()
        val pageable = query.pageable

        val activeGameCountExpr = Sum(
            Case().When(GameTable.active eq true, longLiteral(1)).Else(longLiteral(0)),
            LongColumnType()
        )
        val inactiveGameCountExpr = Sum(
            Case().When(GameTable.active eq false, longLiteral(1)).Else(longLiteral(0)),
            LongColumnType()
        )

        val rows = ProviderTable
            .join(AggregatorTable, JoinType.INNER, ProviderTable.aggregator, AggregatorTable.id)
            .join(GameTable, JoinType.LEFT, ProviderTable.id, GameTable.provider)
            .select(
                ProviderTable.columns +
                        AggregatorTable.columns +
                        activeGameCountExpr +
                        inactiveGameCountExpr
            )
            .where { filterCondition }
            .groupBy(*(ProviderTable.columns + AggregatorTable.columns).toTypedArray())
            .orderBy(ProviderTable.sortOrder)
            .limit(pageable.sizeReal)
            .offset(pageable.offset)
            .toList()

        val items = rows.map { row ->
            ProviderItem(
                provider = row.toProvider(),
                gameActiveCount = row[activeGameCountExpr] ?: 0L,
                gameDeactivateCount = row[inactiveGameCountExpr] ?: 0L,
            )
        }

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
        }
        return conditions.reduceOrNull { acc, op -> acc and op } ?: Op.TRUE
    }
}
