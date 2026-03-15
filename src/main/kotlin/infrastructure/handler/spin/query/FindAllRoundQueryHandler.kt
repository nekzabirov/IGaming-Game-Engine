package com.nekgamebling.infrastructure.handler.spin.query

import application.port.inbound.QueryHandler
import com.nekgamebling.application.port.inbound.game.query.GameItemView
import com.nekgamebling.application.port.inbound.spin.FindAllRoundQuery
import com.nekgamebling.application.port.inbound.spin.FindAllRoundQueryResult
import com.nekgamebling.application.port.inbound.spin.RoundItem
import domain.common.value.SpinType
import domain.session.model.Round
import infrastructure.persistence.exposed.mapper.*
import infrastructure.persistence.exposed.table.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import shared.value.Currency
import shared.value.Page
import java.util.UUID

class FindAllRoundQueryHandler : QueryHandler<FindAllRoundQuery, FindAllRoundQueryResult> {

    override suspend fun handle(query: FindAllRoundQuery): Result<FindAllRoundQueryResult> = newSuspendedTransaction {
        // Check if we need amount filtering (requires joining with spin aggregations)
        val needsAmountFilter = query.minPlaceAmount != null || query.maxPlaceAmount != null ||
                query.minSettleAmount != null || query.maxSettleAmount != null

        // Build base query with JOINs
        val baseQuery = RoundTable
            .innerJoin(SessionTable, { RoundTable.sessionId }, { SessionTable.id })
            .innerJoin(GameTable, { RoundTable.gameId }, { GameTable.id })
            .innerJoin(ProviderTable, { GameTable.providerId }, { ProviderTable.id })
            .leftJoin(SpinTable, { RoundTable.id }, { SpinTable.roundId })

        // Apply filters
        val conditions = mutableListOf<Op<Boolean>>()

        query.gameIdentity?.let { gameIdentity ->
            conditions.add(GameTable.identity eq gameIdentity)
        }

        query.providerIdentity?.let { providerIdentity ->
            conditions.add(ProviderTable.identity eq providerIdentity)
        }

        query.finished?.let { finished ->
            conditions.add(RoundTable.finished eq finished)
        }

        query.playerId?.let { playerId ->
            conditions.add(SessionTable.playerId eq playerId)
        }

        query.freeSpinId?.let { freeSpinId ->
            conditions.add(SpinTable.freeSpinId eq freeSpinId)
        }

        query.startAt?.let { startAt ->
            conditions.add(RoundTable.createdAt greaterEq startAt)
        }

        query.endAt?.let { endAt ->
            conditions.add(RoundTable.createdAt lessEq endAt)
        }

        val whereClause = if (conditions.isNotEmpty()) {
            conditions.reduce { acc, op -> acc and op }
        } else {
            Op.TRUE
        }

        // Helper function to build amount-filtered round IDs query
        fun buildAmountFilteredQuery(): List<UUID> {
            // First, get all round IDs matching base filters
            val candidateRoundIds = RoundTable
                .innerJoin(SessionTable, { RoundTable.sessionId }, { SessionTable.id })
                .innerJoin(GameTable, { RoundTable.gameId }, { GameTable.id })
                .innerJoin(ProviderTable, { GameTable.providerId }, { ProviderTable.id })
                .let { baseJoin ->
                    if (query.freeSpinId != null) {
                        baseJoin.leftJoin(SpinTable, { RoundTable.id }, { SpinTable.roundId })
                    } else {
                        baseJoin
                    }
                }
                .select(RoundTable.id)
                .where { whereClause }
                .withDistinct()
                .map { it[RoundTable.id].value }

            if (candidateRoundIds.isEmpty()) return emptyList()

            // Get place amounts per round
            val placeAmounts = SpinTable
                .select(
                    SpinTable.roundId,
                    SpinTable.realAmount.sum(),
                    SpinTable.bonusAmount.sum()
                )
                .where { (SpinTable.roundId inList candidateRoundIds) and (SpinTable.type eq SpinType.PLACE) }
                .groupBy(SpinTable.roundId)
                .associate { row ->
                    row[SpinTable.roundId]!!.value to (
                        (row[SpinTable.realAmount.sum()] ?: 0L) + (row[SpinTable.bonusAmount.sum()] ?: 0L)
                    )
                }

            // Get settle amounts per round
            val settleAmounts = SpinTable
                .select(
                    SpinTable.roundId,
                    SpinTable.realAmount.sum(),
                    SpinTable.bonusAmount.sum()
                )
                .where { (SpinTable.roundId inList candidateRoundIds) and (SpinTable.type eq SpinType.SETTLE) }
                .groupBy(SpinTable.roundId)
                .associate { row ->
                    row[SpinTable.roundId]!!.value to (
                        (row[SpinTable.realAmount.sum()] ?: 0L) + (row[SpinTable.bonusAmount.sum()] ?: 0L)
                    )
                }

            // Filter by amount conditions
            return candidateRoundIds.filter { roundId ->
                val placeTotal = placeAmounts[roundId] ?: 0L
                val settleTotal = settleAmounts[roundId] ?: 0L

                val passesMinPlace = query.minPlaceAmount?.let { placeTotal >= it } ?: true
                val passesMaxPlace = query.maxPlaceAmount?.let { placeTotal <= it } ?: true
                val passesMinSettle = query.minSettleAmount?.let { settleTotal >= it } ?: true
                val passesMaxSettle = query.maxSettleAmount?.let { settleTotal <= it } ?: true

                passesMinPlace && passesMaxPlace && passesMinSettle && passesMaxSettle
            }
        }

        // Get filtered round IDs (either with or without amount filtering)
        val filteredRoundIds = if (needsAmountFilter) {
            buildAmountFilteredQuery()
        } else {
            null // Will use standard query below
        }

        // Count total items for pagination
        val totalItems = if (needsAmountFilter) {
            filteredRoundIds!!.size.toLong()
        } else {
            RoundTable
                .innerJoin(SessionTable, { RoundTable.sessionId }, { SessionTable.id })
                .innerJoin(GameTable, { RoundTable.gameId }, { GameTable.id })
                .innerJoin(ProviderTable, { GameTable.providerId }, { ProviderTable.id })
                .let { baseJoin ->
                    if (query.freeSpinId != null) {
                        baseJoin.leftJoin(SpinTable, { RoundTable.id }, { SpinTable.roundId })
                    } else {
                        baseJoin
                    }
                }
                .select(RoundTable.id.countDistinct())
                .where { whereClause }
                .first()[RoundTable.id.countDistinct()]
        }

        val totalPages = query.pageable.getTotalPages(totalItems)

        // Get round IDs with pagination
        val roundIds = if (needsAmountFilter) {
            // Apply pagination to pre-filtered IDs
            // First get ordered IDs
            val orderedIds = if (filteredRoundIds!!.isNotEmpty()) {
                RoundTable
                    .select(RoundTable.id)
                    .where { RoundTable.id inList filteredRoundIds }
                    .orderBy(RoundTable.createdAt to SortOrder.DESC)
                    .map { it[RoundTable.id].value }
            } else {
                emptyList()
            }

            orderedIds
                .drop(query.pageable.offset.toInt())
                .take(query.pageable.sizeReal)
        } else {
            RoundTable
                .innerJoin(SessionTable, { RoundTable.sessionId }, { SessionTable.id })
                .innerJoin(GameTable, { RoundTable.gameId }, { GameTable.id })
                .innerJoin(ProviderTable, { GameTable.providerId }, { ProviderTable.id })
                .let { baseJoin ->
                    if (query.freeSpinId != null) {
                        baseJoin.leftJoin(SpinTable, { RoundTable.id }, { SpinTable.roundId })
                    } else {
                        baseJoin
                    }
                }
                .select(RoundTable.id)
                .where { whereClause }
                .groupBy(RoundTable.id)
                .orderBy(RoundTable.createdAt to SortOrder.DESC)
                .limit(query.pageable.sizeReal)
                .offset(query.pageable.offset.toLong())
                .map { it[RoundTable.id].value }
        }

        if (roundIds.isEmpty()) {
            return@newSuspendedTransaction Result.success(
                FindAllRoundQueryResult(
                    items = Page.empty(),
                    providers = emptyList(),
                    aggregators = emptyList(),
                    collections = emptyList()
                )
            )
        }

        // Data class to hold round with related details
        data class RoundWithDetails(
            val round: Round,
            val gameId: UUID,
            val providerId: UUID,
            val game: GameItemView,
            val playerId: String,
            val currency: Currency
        )

        // Get rounds with all details (including variant and aggregator)
        val roundsWithDetails = RoundTable
            .innerJoin(SessionTable, { RoundTable.sessionId }, { SessionTable.id })
            .innerJoin(GameTable, { RoundTable.gameId }, { GameTable.id })
            .innerJoin(ProviderTable, { GameTable.providerId }, { ProviderTable.id })
            .innerJoin(AggregatorInfoTable, { AggregatorInfoTable.id }, { ProviderTable.aggregatorId })
            .innerJoin(GameVariantTable, { GameVariantTable.gameId }, { GameTable.id }) {
                GameVariantTable.aggregator eq AggregatorInfoTable.aggregator
            }
            .selectAll()
            .where { RoundTable.id inList roundIds }
            .orderBy(RoundTable.createdAt to SortOrder.DESC)
            .toList()

        // Get game IDs for collection lookup
        val gameIds = roundsWithDetails.map { it[GameTable.id].value }.distinct()

        // Fetch collection identities for all games in one query
        val gameCollections = if (gameIds.isNotEmpty()) {
            CollectionGameTable
                .innerJoin(CollectionTable, { CollectionTable.id }, { CollectionGameTable.categoryId })
                .select(CollectionGameTable.gameId, CollectionTable.identity)
                .where { CollectionGameTable.gameId inList gameIds }
                .groupBy { it[CollectionGameTable.gameId].value }
                .mapValues { entry -> entry.value.map { it[CollectionTable.identity] } }
        } else {
            emptyMap()
        }

        val roundDetails = roundsWithDetails.map { row ->
            val gameId = row[GameTable.id].value
            RoundWithDetails(
                round = Round(
                    id = row[RoundTable.id].value,
                    sessionId = row[RoundTable.sessionId].value,
                    gameId = row[RoundTable.gameId].value,
                    extId = row[RoundTable.extId],
                    finished = row[RoundTable.finished],
                    createdAt = row[RoundTable.createdAt],
                    finishedAt = row[RoundTable.finishedAt]
                ),
                gameId = gameId,
                providerId = row[ProviderTable.id].value,
                game = GameItemView(
                    game = row.toGame(),
                    activeVariant = row.toGameVariant(),
                    collectionIdentities = gameCollections[gameId] ?: emptyList()
                ),
                playerId = row[SessionTable.playerId],
                currency = Currency(row[SessionTable.currency])
            )
        }

        // Get spin aggregations per round
        val placeAmounts = SpinTable
            .select(
                SpinTable.roundId,
                SpinTable.realAmount.sum(),
                SpinTable.bonusAmount.sum()
            )
            .where { (SpinTable.roundId inList roundIds) and (SpinTable.type eq SpinType.PLACE) }
            .groupBy(SpinTable.roundId)
            .associate { row ->
                row[SpinTable.roundId]!!.value to Pair(
                    row[SpinTable.realAmount.sum()] ?: 0L,
                    row[SpinTable.bonusAmount.sum()] ?: 0L
                )
            }

        val settleAmounts = SpinTable
            .select(
                SpinTable.roundId,
                SpinTable.realAmount.sum(),
                SpinTable.bonusAmount.sum()
            )
            .where { (SpinTable.roundId inList roundIds) and (SpinTable.type eq SpinType.SETTLE) }
            .groupBy(SpinTable.roundId)
            .associate { row ->
                row[SpinTable.roundId]!!.value to Pair(
                    row[SpinTable.realAmount.sum()] ?: 0L,
                    row[SpinTable.bonusAmount.sum()] ?: 0L
                )
            }

        // Build round items
        val items = roundDetails.map { details ->
            val placeAmt = placeAmounts[details.round.id] ?: Pair(0L, 0L)
            val settleAmt = settleAmounts[details.round.id] ?: Pair(0L, 0L)
            RoundItem(
                round = details.round,
                game = details.game,
                playerId = details.playerId,
                currency = details.currency,
                totalPlaceReal = placeAmt.first,
                totalPlaceBonus = placeAmt.second,
                totalSettleReal = settleAmt.first,
                totalSettleBonus = settleAmt.second
            )
        }

        // Fetch distinct providers from results
        val providerIds = roundsWithDetails.map { it[ProviderTable.id].value }.distinct()
        val providers = if (providerIds.isNotEmpty()) {
            ProviderTable
                .selectAll()
                .where { ProviderTable.id inList providerIds }
                .map { it.toProvider() }
        } else {
            emptyList()
        }

        // Fetch distinct aggregators from results
        val aggregatorIds = roundsWithDetails.map { it[AggregatorInfoTable.id].value }.distinct()
        val aggregators = if (aggregatorIds.isNotEmpty()) {
            AggregatorInfoTable
                .selectAll()
                .where { AggregatorInfoTable.id inList aggregatorIds }
                .map { it.toAggregatorInfo() }
        } else {
            emptyList()
        }

        // Fetch collections that appear in the results
        val allCollectionIdentities = gameCollections.values.flatten().distinct()
        val collections = if (allCollectionIdentities.isNotEmpty()) {
            CollectionTable
                .selectAll()
                .where { CollectionTable.identity inList allCollectionIdentities }
                .map { it.toCollection() }
        } else {
            emptyList()
        }

        Result.success(
            FindAllRoundQueryResult(
                items = Page(
                    items = items,
                    totalPages = totalPages,
                    totalItems = totalItems,
                    currentPage = query.pageable.pageReal
                ),
                providers = providers,
                aggregators = aggregators,
                collections = collections
            )
        )
    }
}
