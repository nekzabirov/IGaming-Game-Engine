package com.nekgamebling.infrastructure.handler.spin.query

import application.port.inbound.QueryHandler
import com.nekgamebling.application.port.inbound.game.query.GameItemView
import com.nekgamebling.application.port.inbound.spin.FindRoundQuery
import com.nekgamebling.application.port.inbound.spin.FindRoundQueryResult
import domain.common.error.NotFoundError
import domain.common.value.SpinType
import domain.session.model.Round
import infrastructure.persistence.exposed.mapper.*
import infrastructure.persistence.exposed.table.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import shared.value.Currency
import java.util.UUID

class FindRoundQueryHandler : QueryHandler<FindRoundQuery, FindRoundQueryResult> {

    override suspend fun handle(query: FindRoundQuery): Result<FindRoundQueryResult> = newSuspendedTransaction {
        val roundId = try {
            UUID.fromString(query.id)
        } catch (e: IllegalArgumentException) {
            return@newSuspendedTransaction Result.failure(NotFoundError("Round", query.id))
        }

        // Get round with related details
        val row = RoundTable
            .innerJoin(SessionTable, { RoundTable.sessionId }, { SessionTable.id })
            .innerJoin(GameTable, { RoundTable.gameId }, { GameTable.id })
            .innerJoin(ProviderTable, { GameTable.providerId }, { ProviderTable.id })
            .innerJoin(AggregatorInfoTable, { AggregatorInfoTable.id }, { ProviderTable.aggregatorId })
            .innerJoin(GameVariantTable, { GameVariantTable.gameId }, { GameTable.id }) {
                GameVariantTable.aggregator eq AggregatorInfoTable.aggregator
            }
            .selectAll()
            .where { RoundTable.id eq roundId }
            .firstOrNull()
            ?: return@newSuspendedTransaction Result.failure(NotFoundError("Round", query.id))

        val round = Round(
            id = row[RoundTable.id].value,
            sessionId = row[RoundTable.sessionId].value,
            gameId = row[RoundTable.gameId].value,
            extId = row[RoundTable.extId],
            finished = row[RoundTable.finished],
            createdAt = row[RoundTable.createdAt],
            finishedAt = row[RoundTable.finishedAt]
        )

        // Get collection identities for this game
        val gameId = row[GameTable.id].value
        val collectionIdentities = CollectionGameTable
            .innerJoin(CollectionTable, { CollectionTable.id }, { CollectionGameTable.categoryId })
            .select(CollectionTable.identity)
            .where { CollectionGameTable.gameId eq gameId }
            .map { it[CollectionTable.identity] }

        // Get spin aggregations
        val placeAmounts = SpinTable
            .select(SpinTable.realAmount.sum(), SpinTable.bonusAmount.sum())
            .where { (SpinTable.roundId eq roundId) and (SpinTable.type eq SpinType.PLACE) }
            .firstOrNull()
            ?.let {
                Pair(it[SpinTable.realAmount.sum()] ?: 0L, it[SpinTable.bonusAmount.sum()] ?: 0L)
            } ?: Pair(0L, 0L)

        val settleAmounts = SpinTable
            .select(SpinTable.realAmount.sum(), SpinTable.bonusAmount.sum())
            .where { (SpinTable.roundId eq roundId) and (SpinTable.type eq SpinType.SETTLE) }
            .firstOrNull()
            ?.let {
                Pair(it[SpinTable.realAmount.sum()] ?: 0L, it[SpinTable.bonusAmount.sum()] ?: 0L)
            } ?: Pair(0L, 0L)

        // Fetch provider and aggregator
        val provider = row.toProvider()
        val aggregator = row.toAggregatorInfo()

        // Fetch collections
        val collections = if (collectionIdentities.isNotEmpty()) {
            CollectionTable
                .selectAll()
                .where { CollectionTable.identity inList collectionIdentities }
                .map { it.toCollection() }
        } else {
            emptyList()
        }

        Result.success(
            FindRoundQueryResult(
                round = round,
                game = GameItemView(
                    game = row.toGame(),
                    activeVariant = row.toGameVariant(),
                    collectionIdentities = collectionIdentities
                ),
                playerId = row[SessionTable.playerId],
                currency = Currency(row[SessionTable.currency]),
                totalPlaceReal = placeAmounts.first,
                totalPlaceBonus = placeAmounts.second,
                totalSettleReal = settleAmounts.first,
                totalSettleBonus = settleAmounts.second,
                providers = listOf(provider),
                aggregators = listOf(aggregator),
                collections = collections
            )
        )
    }
}
