package infrastructure.handler.provider

import application.IQueryHandler
import application.query.provider.FindProviderQuery
import domain.model.Provider
import infrastructure.persistence.dbRead
import infrastructure.persistence.mapper.ProviderMapper.toProvider
import infrastructure.persistence.table.AggregatorTable
import infrastructure.persistence.table.ProviderTable
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.selectAll
import java.util.Optional

class FindProviderQueryHandler : IQueryHandler<FindProviderQuery, Optional<Provider>> {

    override suspend fun handle(query: FindProviderQuery): Optional<Provider> = dbRead {
        Optional.ofNullable(
            ProviderTable
                .join(AggregatorTable, JoinType.INNER, ProviderTable.aggregator, AggregatorTable.id)
                .selectAll()
                .where { ProviderTable.identity eq query.identity.value }
                .singleOrNull()
                ?.toProvider()
        )
    }
}
