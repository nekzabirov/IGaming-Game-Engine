package infrastructure.handler.provider

import application.cqrs.IQueryHandler
import application.cqrs.provider.FindProviderQuery
import application.cqrs.provider.ProviderItem
import infrastructure.persistence.mapper.ProviderMapper.toProvider
import infrastructure.persistence.table.AggregatorTable
import infrastructure.persistence.table.GameTable
import infrastructure.persistence.table.ProviderTable
import org.jetbrains.exposed.sql.Case
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.LongColumnType
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Sum
import org.jetbrains.exposed.sql.longLiteral
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.Optional

class FindProviderQueryHandler : IQueryHandler<FindProviderQuery, Optional<ProviderItem>> {

    override suspend fun handle(query: FindProviderQuery): Optional<ProviderItem> = newSuspendedTransaction {
        val activeGameCountExpr = Sum(
            Case()
                .When(GameTable.active eq true, longLiteral(1))
                .Else(longLiteral(0)),
            LongColumnType()
        )

        val inactiveGameCountExpr = Sum(
            Case()
                .When(GameTable.active eq false, longLiteral(1))
                .Else(longLiteral(0)),
            LongColumnType()
        )

        val row = ProviderTable
            .join(AggregatorTable, JoinType.INNER, ProviderTable.aggregator, AggregatorTable.id)
            .join(GameTable, JoinType.LEFT, ProviderTable.id, GameTable.provider)
            .select(
                ProviderTable.columns +
                        AggregatorTable.columns +
                        activeGameCountExpr +
                        inactiveGameCountExpr
            )
            .where { ProviderTable.identity eq query.identity.value }
            .groupBy(*(ProviderTable.columns + AggregatorTable.columns).toTypedArray())
            .singleOrNull()
            ?: return@newSuspendedTransaction Optional.empty()

        Optional.of(
            ProviderItem(
                provider = row.toProvider(),
                gameActiveCount = row[activeGameCountExpr] ?: 0L,
                gameDeactivateCount = row[inactiveGameCountExpr] ?: 0L,
            )
        )
    }
}
