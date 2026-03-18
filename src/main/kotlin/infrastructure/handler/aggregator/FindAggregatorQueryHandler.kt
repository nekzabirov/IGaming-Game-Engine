package infrastructure.handler.aggregator

import application.cqrs.aggregator.FindAggregatorQuery
import application.cqrs.IQueryHandler
import domain.model.Aggregator
import infrastructure.persistence.mapper.AggregatorMapper.toAggregator
import infrastructure.persistence.table.AggregatorTable
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.Optional

class FindAggregatorQueryHandler : IQueryHandler<FindAggregatorQuery, Optional<Aggregator>> {

    override suspend fun handle(query: FindAggregatorQuery): Optional<Aggregator> = newSuspendedTransaction {
        Optional.ofNullable(
            AggregatorTable
                .selectAll()
                .where { AggregatorTable.identity eq query.identity.value }
                .singleOrNull()
                ?.toAggregator()
        )
    }
}
