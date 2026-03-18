package infrastructure.handler.collection

import application.cqrs.collection.CollectionItem
import application.cqrs.IQueryHandler
import application.cqrs.collection.FindCollectionQuery
import infrastructure.persistence.mapper.CollectionMapper.toCollection
import infrastructure.persistence.table.CollectionTable
import infrastructure.persistence.table.GameCollectionTable
import infrastructure.persistence.table.GameTable
import org.jetbrains.exposed.sql.Case
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.LongColumnType
import org.jetbrains.exposed.sql.Sum
import org.jetbrains.exposed.sql.countDistinct
import org.jetbrains.exposed.sql.longLiteral
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.Optional

class FindCollectionQueryHandler : IQueryHandler<FindCollectionQuery, Optional<CollectionItem>> {

    override suspend fun handle(query: FindCollectionQuery): Optional<CollectionItem> = newSuspendedTransaction {
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

        val providerCountExpr = GameTable.provider.countDistinct()

        val row = CollectionTable
            .join(GameCollectionTable, JoinType.LEFT, CollectionTable.id, GameCollectionTable.collection)
            .join(GameTable, JoinType.LEFT, GameCollectionTable.game, GameTable.id)
            .select(
                CollectionTable.columns +
                        activeGameCountExpr +
                        inactiveGameCountExpr +
                        providerCountExpr
            )
            .where { CollectionTable.identity eq query.identity.value }
            .groupBy(*CollectionTable.columns.toTypedArray())
            .singleOrNull()
            ?: return@newSuspendedTransaction Optional.empty()

        Optional.of(
            CollectionItem(
                collection = row.toCollection(),
                gameActiveCount = row[activeGameCountExpr] ?: 0L,
                gameDeactivateCount = row[inactiveGameCountExpr] ?: 0L,
                providerCount = row[providerCountExpr],
            )
        )
    }
}
