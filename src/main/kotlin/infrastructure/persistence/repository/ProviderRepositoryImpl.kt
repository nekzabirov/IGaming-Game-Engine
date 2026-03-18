package infrastructure.persistence.repository

import application.port.storage.IProviderRepository
import domain.model.Provider
import domain.vo.Identity
import domain.vo.Page
import domain.vo.Pageable
import infrastructure.persistence.mapper.ProviderMapper.toProvider
import infrastructure.persistence.table.AggregatorTable
import infrastructure.persistence.table.ProviderTable
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.batchUpsert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.upsert

class ProviderRepositoryImpl : IProviderRepository {

    override suspend fun save(provider: Provider): Provider = newSuspendedTransaction {
        val aggregatorId = AggregatorTable.select(AggregatorTable.id)
            .where { AggregatorTable.identity eq provider.aggregator.identity.value }
            .singleOrNull()?.get(AggregatorTable.id)
            ?: error("Aggregator not found: ${provider.aggregator.identity.value}")

        ProviderTable.upsert(keys = arrayOf(ProviderTable.identity)) {
            it[identity] = provider.identity.value
            it[name] = provider.name
            it[images] = provider.images.data
            it[sortOrder] = provider.order
            it[active] = provider.active
            it[aggregator] = aggregatorId
        }

        provider
    }

    override suspend fun saveAll(providers: List<Provider>): List<Provider> = newSuspendedTransaction {
        val aggregatorIdentities = providers.map { it.aggregator.identity.value }.distinct()
        val aggregatorMap = AggregatorTable.select(AggregatorTable.id, AggregatorTable.identity)
            .where { AggregatorTable.identity inList aggregatorIdentities }
            .associate { it[AggregatorTable.identity] to it[AggregatorTable.id] }

        ProviderTable.batchUpsert(providers, keys = arrayOf(ProviderTable.identity)) { provider ->
            val aggregatorId = aggregatorMap[provider.aggregator.identity.value]
                ?: error("Aggregator not found: ${provider.aggregator.identity.value}")

            this[ProviderTable.identity] = provider.identity.value
            this[ProviderTable.name] = provider.name
            this[ProviderTable.images] = provider.images.data
            this[ProviderTable.sortOrder] = provider.order
            this[ProviderTable.active] = provider.active
            this[ProviderTable.aggregator] = aggregatorId
        }

        providers
    }

    override suspend fun findAll(): List<Provider> = newSuspendedTransaction {
        ProviderTable
            .join(AggregatorTable, JoinType.INNER, ProviderTable.aggregator, AggregatorTable.id)
            .selectAll()
            .map { it.toProvider() }
    }

    override suspend fun findByIdentity(identity: Identity): Provider? = newSuspendedTransaction {
        ProviderTable
            .join(AggregatorTable, JoinType.INNER, ProviderTable.aggregator, AggregatorTable.id)
            .selectAll()
            .where { ProviderTable.identity eq identity.value }
            .singleOrNull()
            ?.toProvider()
    }

    override suspend fun findAll(pageable: Pageable): Page<Provider> = newSuspendedTransaction {
        val totalItems = ProviderTable.selectAll().count()
        val items = ProviderTable
            .join(AggregatorTable, JoinType.INNER, ProviderTable.aggregator, AggregatorTable.id)
            .selectAll()
            .limit(pageable.sizeReal, pageable.offset)
            .map { it.toProvider() }

        Page(
            items = items,
            totalPages = pageable.getTotalPages(totalItems),
            totalItems = totalItems,
            currentPage = pageable.pageReal,
        )
    }
}
