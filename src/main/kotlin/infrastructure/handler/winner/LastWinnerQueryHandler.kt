package infrastructure.handler.winner

import application.cqrs.IQueryHandler
import application.cqrs.winner.LastWin
import application.cqrs.winner.LastWinnerQuery
import domain.model.SpinType
import domain.vo.Amount
import domain.vo.Currency
import domain.vo.Page
import domain.vo.PlayerId
import infrastructure.persistence.mapper.GameMapper.toGame
import infrastructure.persistence.table.AggregatorTable
import infrastructure.persistence.table.GameTable
import infrastructure.persistence.table.GameVariantTable
import infrastructure.persistence.table.ProviderTable
import infrastructure.persistence.table.RoundTable
import infrastructure.persistence.table.SessionTable
import infrastructure.persistence.table.SpinTable
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class LastWinnerQueryHandler : IQueryHandler<LastWinnerQuery, Page<LastWin>> {

    override suspend fun handle(query: LastWinnerQuery): Page<LastWin> = newSuspendedTransaction {
        val baseQuery = SpinTable
            .innerJoin(RoundTable)
            .join(SessionTable, JoinType.INNER, RoundTable.session, SessionTable.id)
            .join(GameVariantTable, JoinType.INNER, RoundTable.gameVariant, GameVariantTable.id)
            .join(GameTable, JoinType.INNER, GameVariantTable.game, GameTable.id)
            .join(ProviderTable, JoinType.INNER, GameTable.provider, ProviderTable.id)
            .join(AggregatorTable, JoinType.INNER, ProviderTable.aggregator, AggregatorTable.id)
            .select(
                SpinTable.amount,
                RoundTable.createdAt,
                SessionTable.currency,
                SessionTable.playerId,
                GameTable.identity,
                GameTable.name,
                GameTable.bonusBetEnable,
                GameTable.bonusWageringEnable,
                GameTable.tags,
                GameTable.active,
                GameTable.images,
                GameTable.sortOrder,
                ProviderTable.identity,
                ProviderTable.name,
                ProviderTable.images,
                ProviderTable.sortOrder,
                ProviderTable.active,
                AggregatorTable.identity,
                AggregatorTable.integration,
                AggregatorTable.config,
                AggregatorTable.active,
            )
            .where {
                (SpinTable.type eq SpinType.SETTLE) and (RoundTable.freespinId.isNull())
            }

        query.gameIdentity?.let {
            baseQuery.andWhere { GameTable.identity eq it.value }
        }

        query.minAmount?.let {
            baseQuery.andWhere { SpinTable.amount greaterEq it.value }
        }

        query.maxAmount?.let {
            baseQuery.andWhere { SpinTable.amount lessEq it.value }
        }

        query.currency?.let {
            baseQuery.andWhere { SessionTable.currency eq it.value }
        }

        query.playerId?.let {
            baseQuery.andWhere { SessionTable.playerId eq it.value }
        }

        query.fromDate?.let {
            baseQuery.andWhere { RoundTable.createdAt greaterEq it }
        }

        query.toDate?.let {
            baseQuery.andWhere { RoundTable.createdAt lessEq it }
        }

        val totalItems = baseQuery.count()
        val pageable = query.pageable

        val rows = baseQuery
            .orderBy(RoundTable.createdAt, SortOrder.DESC)
            .limit(pageable.sizeReal)
            .offset(pageable.offset)
            .toList()

        val spins = rows.map { row ->
            LastWin(
                game = row.toGame(),
                amount = Amount(row[SpinTable.amount]),
                currency = Currency(row[SessionTable.currency]),
                playerId = PlayerId(row[SessionTable.playerId]),
                date = row[RoundTable.createdAt],
            )
        }

        Page(
            items = spins,
            totalPages = pageable.getTotalPages(totalItems),
            totalItems = totalItems,
            currentPage = pageable.pageReal,
        )
    }
}
