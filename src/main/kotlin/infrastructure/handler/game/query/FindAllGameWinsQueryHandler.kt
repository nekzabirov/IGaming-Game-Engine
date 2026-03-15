package com.nekgamebling.infrastructure.handler.game.query

import application.port.inbound.QueryHandler
import com.nekgamebling.application.port.inbound.game.query.FindAllGameWinsQuery
import com.nekgamebling.application.port.inbound.game.query.FindAllGameWinsResponse
import com.nekgamebling.application.port.inbound.game.query.GameItemView
import com.nekgamebling.application.port.inbound.game.query.GameWonItem
import infrastructure.persistence.exposed.mapper.*
import infrastructure.persistence.exposed.table.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import shared.value.Page

class FindAllGameWinsQueryHandler : QueryHandler<FindAllGameWinsQuery, FindAllGameWinsResponse> {

    override suspend fun handle(query: FindAllGameWinsQuery): Result<FindAllGameWinsResponse> = newSuspendedTransaction {
        val baseJoin = GameWonTable
            .innerJoin(GameTable, { GameWonTable.gameId }, { GameTable.id })
            .innerJoin(ProviderTable, { ProviderTable.id }, { GameTable.providerId })
            .innerJoin(AggregatorInfoTable, { AggregatorInfoTable.id }, { ProviderTable.aggregatorId })
            .innerJoin(GameVariantTable, { GameVariantTable.gameId }, { GameTable.id }) {
                GameVariantTable.aggregator eq AggregatorInfoTable.aggregator
            }

        val conditions = mutableListOf<Op<Boolean>>()

        query.gameIdentity?.let { conditions.add(GameTable.identity eq it) }
        query.playerId?.let { conditions.add(GameWonTable.playerId eq it) }
        query.currency?.let { conditions.add(GameWonTable.currency eq it) }
        query.minAmount?.let { conditions.add(GameWonTable.amount greaterEq it) }
        query.maxAmount?.let { conditions.add(GameWonTable.amount lessEq it) }
        query.startAt?.let { conditions.add(GameWonTable.createdAt greaterEq it) }
        query.endAt?.let { conditions.add(GameWonTable.createdAt lessEq it) }

        val whereClause = if (conditions.isNotEmpty()) {
            conditions.reduce { acc, op -> acc and op }
        } else {
            Op.TRUE
        }

        val totalItems = baseJoin
            .select(GameWonTable.id.count())
            .where { whereClause }
            .first()[GameWonTable.id.count()]

        val totalPages = query.pageable.getTotalPages(totalItems)

        val rows = baseJoin
            .selectAll()
            .where { whereClause }
            .orderBy(GameWonTable.createdAt to SortOrder.DESC)
            .limit(query.pageable.sizeReal)
            .offset(query.pageable.offset)
            .toList()

        // Get game IDs for collection lookup
        val gameIds = rows.map { it[GameTable.id].value }.distinct()

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

        val items = rows.map { row ->
            val gameId = row[GameTable.id].value
            GameWonItem(
                id = row[GameWonTable.id].value.toString(),
                game = GameItemView(
                    game = row.toGame(),
                    activeVariant = row.toGameVariant(),
                    collectionIdentities = gameCollections[gameId] ?: emptyList()
                ),
                playerId = row[GameWonTable.playerId],
                amount = row[GameWonTable.amount],
                currency = row[GameWonTable.currency],
                createdAt = row[GameWonTable.createdAt]
            )
        }

        // Fetch distinct providers from results
        val providerIds = rows.map { it[ProviderTable.id].value }.distinct()
        val providers = if (providerIds.isNotEmpty()) {
            ProviderTable
                .selectAll()
                .where { ProviderTable.id inList providerIds }
                .map { it.toProvider() }
        } else {
            emptyList()
        }

        // Fetch distinct aggregators from results
        val aggregatorIds = rows.map { it[AggregatorInfoTable.id].value }.distinct()
        val aggregators = if (aggregatorIds.isNotEmpty()) {
            AggregatorInfoTable
                .selectAll()
                .where { AggregatorInfoTable.id inList aggregatorIds }
                .map { it.toAggregatorInfo() }
        } else {
            emptyList()
        }

        // Fetch collections that appear in the results
        val collectionIdentities = gameCollections.values.flatten().distinct()
        val collections = if (collectionIdentities.isNotEmpty()) {
            CollectionTable
                .selectAll()
                .where { CollectionTable.identity inList collectionIdentities }
                .map { it.toCollection() }
        } else {
            emptyList()
        }

        Result.success(
            FindAllGameWinsResponse(
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
