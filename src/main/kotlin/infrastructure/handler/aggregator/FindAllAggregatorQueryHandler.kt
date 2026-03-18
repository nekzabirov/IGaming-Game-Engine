package infrastructure.handler.aggregator

import application.cqrs.aggregator.FindAllAggregatorQuery
import application.cqrs.IQueryHandler
import domain.model.Aggregator
import domain.vo.Page
import infrastructure.persistence.mapper.AggregatorMapper.toAggregator
import infrastructure.persistence.table.AggregatorTable
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class FindAllAggregatorQueryHandler : IQueryHandler<FindAllAggregatorQuery, Page<Aggregator>> {

    override suspend fun handle(query: FindAllAggregatorQuery): Page<Aggregator> = newSuspendedTransaction {
        val baseQuery = AggregatorTable
            .selectAll()
            .where { AggregatorTable.identity like "%${query.query.lowercase()}%" }

        query.integration?.let {
            baseQuery.andWhere { AggregatorTable.integration eq it }
        }

        query.active?.let {
            baseQuery.andWhere { AggregatorTable.active eq it }
        }

        val totalItems = baseQuery.count()
        val pageable = query.pageable

        val items = baseQuery
            .limit(pageable.sizeReal)
            .offset(pageable.offset)
            .map { it.toAggregator() }

        Page(
            items = items,
            totalPages = pageable.getTotalPages(totalItems),
            totalItems = totalItems,
            currentPage = pageable.pageReal,
        )
    }
}
