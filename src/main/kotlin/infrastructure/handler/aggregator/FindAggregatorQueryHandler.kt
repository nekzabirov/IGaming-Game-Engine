package infrastructure.handler.aggregator

import application.query.aggregator.FindAggregatorQuery
import application.IQueryHandler
import domain.model.Aggregator
import infrastructure.persistence.mapper.AggregatorMapper.toAggregator
import infrastructure.persistence.table.AggregatorTable
import org.jetbrains.exposed.sql.selectAll
import infrastructure.persistence.dbRead
import java.util.Optional

class FindAggregatorQueryHandler : IQueryHandler<FindAggregatorQuery, Optional<Aggregator>> {

    override suspend fun handle(query: FindAggregatorQuery): Optional<Aggregator> = dbRead {
        Optional.ofNullable(
            AggregatorTable
                .selectAll()
                .where { AggregatorTable.identity eq query.identity.value }
                .singleOrNull()
                ?.toAggregator()
        )
    }
}
