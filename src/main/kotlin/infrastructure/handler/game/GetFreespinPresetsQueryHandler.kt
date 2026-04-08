package infrastructure.handler.game

import application.IQueryHandler
import application.query.freespin.GetFreespinPresetsQuery
import application.port.factory.IAggregatorFactory
import domain.exception.conflict.FreespinNotSupportedException
import domain.exception.notfound.GameNotFoundException
import infrastructure.persistence.mapper.AggregatorMapper.toAggregator
import infrastructure.persistence.table.AggregatorTable
import infrastructure.persistence.table.GameTable
import infrastructure.persistence.table.GameVariantTable
import infrastructure.persistence.table.ProviderTable
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import infrastructure.persistence.dbRead

class GetFreespinPresetsQueryHandler(
    private val aggregatorFactory: IAggregatorFactory
) : IQueryHandler<GetFreespinPresetsQuery, Map<String, Any>> {

    override suspend fun handle(query: GetFreespinPresetsQuery): Map<String, Any> {
        val (aggregator, variantSymbol) = dbRead {
            val row = GameVariantTable
                .join(GameTable, JoinType.INNER, GameVariantTable.game, GameTable.id)
                .join(ProviderTable, JoinType.INNER, GameTable.provider, ProviderTable.id)
                .join(AggregatorTable, JoinType.INNER, ProviderTable.aggregator, AggregatorTable.id)
                .selectAll()
                .where {
                    (GameTable.identity eq query.gameIdentity.value) and
                            (GameVariantTable.integration eq AggregatorTable.integration)
                }
                .firstOrNull() ?: throw GameNotFoundException()

            if (!row[GameVariantTable.freeSpinEnable]) {
                throw FreespinNotSupportedException()
            }

            row.toAggregator() to row[GameVariantTable.symbol]
        }

        val freespinAdapter = aggregatorFactory.createFreespinAdapter(aggregator)

        return freespinAdapter.getPreset(variantSymbol)
    }
}
