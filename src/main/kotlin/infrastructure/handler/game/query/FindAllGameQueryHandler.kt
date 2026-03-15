package com.nekgamebling.infrastructure.handler.game.query

import application.port.inbound.QueryHandler
import com.nekgamebling.application.port.inbound.game.query.FindAllGameQuery
import com.nekgamebling.application.port.inbound.game.query.FindAllGameResponse
import com.nekgamebling.application.port.inbound.game.query.GameItemView
import infrastructure.persistence.exposed.mapper.*
import infrastructure.persistence.exposed.table.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import shared.value.Page

class FindAllGameQueryHandler : QueryHandler<FindAllGameQuery, FindAllGameResponse> {
    override suspend fun handle(query: FindAllGameQuery): Result<FindAllGameResponse> = newSuspendedTransaction {
        val baseQuery = buildBaseQuery(query)

        // Count total items
        val totalItems = baseQuery.count()

        // Fetch paginated games
        val gameRows = buildBaseQuery(query)
            .orderBy(GameTable.name, SortOrder.ASC)
            .limit(query.pageable.sizeReal)
            .offset(query.pageable.offset)
            .toList()

        // Get game IDs for collection lookup
        val gameIds = gameRows.map { it[GameTable.id].value }

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

        // Map to GameItems
        val gameItems = gameRows.map { row ->
            val gameId = row[GameTable.id].value
            GameItemView(
                game = row.toGame(),
                activeVariant = row.toGameVariant(),
                collectionIdentities = gameCollections[gameId] ?: emptyList()
            )
        }

        // Fetch distinct providers from results
        val providerIds = gameRows.map { it[ProviderTable.id].value }.distinct()
        val providers = if (providerIds.isNotEmpty()) {
            ProviderTable
                .selectAll()
                .where { ProviderTable.id inList providerIds }
                .map { it.toProvider() }
        } else {
            emptyList()
        }

        // Fetch distinct aggregators from results
        val aggregatorIds = gameRows.map { it[AggregatorInfoTable.id].value }.distinct()
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
            FindAllGameResponse(
                result = Page(
                    items = gameItems,
                    totalPages = query.pageable.getTotalPages(totalItems),
                    totalItems = totalItems,
                    currentPage = query.pageable.pageReal
                ),
                providers = providers,
                aggregators = aggregators,
                collections = collections
            )
        )
    }

    private fun buildBaseQuery(query: FindAllGameQuery): Query {
        var baseQuery = GameTable
            .innerJoin(ProviderTable, { ProviderTable.id }, { GameTable.providerId })
            .innerJoin(AggregatorInfoTable, { AggregatorInfoTable.id }, { ProviderTable.aggregatorId })
            .innerJoin(GameVariantTable, { GameVariantTable.gameId }, { GameTable.id }) {
                GameVariantTable.aggregator eq AggregatorInfoTable.aggregator
            }
            .selectAll()

        // Search query on game name or identity
        if (query.query.isNotBlank()) {
            val searchPattern = "%${query.query.lowercase()}%"
            baseQuery = baseQuery.andWhere {
                (GameTable.name.lowerCase() like searchPattern) or
                        (GameTable.identity.lowerCase() like searchPattern)
            }
        }

        // Active filter
        if (query.active != null) {
            baseQuery = baseQuery.andWhere { GameTable.active eq query.active }
        }

        // Provider filter
        val providerIdentities = query.providerIdentities
        if (!providerIdentities.isNullOrEmpty()) {
            baseQuery = baseQuery.andWhere { ProviderTable.identity inList providerIdentities }
        }

        // Collection filter - games that belong to any of the specified collections
        val collectionIdentities = query.collectionIdentities
        if (!collectionIdentities.isNullOrEmpty()) {
            val gameIdsInCollections = CollectionGameTable
                .innerJoin(CollectionTable, { CollectionTable.id }, { CollectionGameTable.categoryId })
                .select(CollectionGameTable.gameId)
                .where { CollectionTable.identity inList collectionIdentities }
                .map { it[CollectionGameTable.gameId] }

            if (gameIdsInCollections.isNotEmpty()) {
                baseQuery = baseQuery.andWhere { GameTable.id inList gameIdsInCollections }
            }
        }

        // Tags filter
        val tags = query.tags
        if (!tags.isNullOrEmpty()) {
            tags.forEach { tag ->
                baseQuery = baseQuery.andWhere { stringParam(tag) eq anyFrom(GameTable.tags) }
            }
        }

        // Boolean filters
        if (query.bonusBetEnable != null) {
            baseQuery = baseQuery.andWhere { GameTable.bonusBetEnable eq query.bonusBetEnable }
        }
        if (query.bonusWageringEnable != null) {
            baseQuery = baseQuery.andWhere { GameTable.bonusWageringEnable eq query.bonusWageringEnable }
        }
        if (query.freeSpinEnable != null) {
            baseQuery = baseQuery.andWhere { GameVariantTable.freeSpinEnable eq query.freeSpinEnable }
        }
        if (query.freeChipEnable != null) {
            baseQuery = baseQuery.andWhere { GameVariantTable.freeChipEnable eq query.freeChipEnable }
        }
        if (query.jackpotEnable != null) {
            baseQuery = baseQuery.andWhere { GameVariantTable.jackpotEnable eq query.jackpotEnable }
        }

        return baseQuery
    }
}
