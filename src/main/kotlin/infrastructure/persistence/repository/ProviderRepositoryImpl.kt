package infrastructure.persistence.repository

import domain.repository.IProviderRepository
import domain.exception.domainRequireNotNull
import domain.exception.notfound.AggregatorNotFoundException
import domain.exception.notfound.ProviderNotFoundException
import domain.model.Provider
import domain.vo.Identity
import domain.vo.Page
import domain.vo.Pageable
import infrastructure.persistence.dbRead
import infrastructure.persistence.dbTransaction
import infrastructure.persistence.entity.ProviderEntity
import infrastructure.persistence.mapper.ProviderMapper.toProvider
import infrastructure.persistence.table.AggregatorTable
import infrastructure.persistence.table.ProviderTable
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.batchUpsert
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.upsert

class ProviderRepositoryImpl : IProviderRepository {

    override suspend fun save(provider: Provider): Provider = dbTransaction {
        val aggregatorId = domainRequireNotNull(
            AggregatorTable.select(AggregatorTable.id)
                .where { AggregatorTable.identity eq provider.aggregator.identity.value }
                .singleOrNull()?.get(AggregatorTable.id)
        ) { AggregatorNotFoundException() }

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

    override suspend fun saveAll(providers: List<Provider>): List<Provider> = dbTransaction {
        val aggregatorIdentities = providers.map { it.aggregator.identity.value }.distinct()
        val aggregatorMap = AggregatorTable.select(AggregatorTable.id, AggregatorTable.identity)
            .where { AggregatorTable.identity inList aggregatorIdentities }
            .associate { it[AggregatorTable.identity] to it[AggregatorTable.id] }

        ProviderTable.batchUpsert(providers, keys = arrayOf(ProviderTable.identity)) { provider ->
            val aggregatorId = domainRequireNotNull(aggregatorMap[provider.aggregator.identity.value]) {
                AggregatorNotFoundException()
            }

            this[ProviderTable.identity] = provider.identity.value
            this[ProviderTable.name] = provider.name
            this[ProviderTable.images] = provider.images.data
            this[ProviderTable.sortOrder] = provider.order
            this[ProviderTable.active] = provider.active
            this[ProviderTable.aggregator] = aggregatorId
        }

        providers
    }

    override suspend fun findAll(): List<Provider> = dbRead {
        ProviderTable
            .join(AggregatorTable, JoinType.INNER, ProviderTable.aggregator, AggregatorTable.id)
            .selectAll()
            .map { it.toProvider() }
    }

    override suspend fun findByIdentity(identity: Identity): Provider? = dbRead {
        ProviderTable
            .join(AggregatorTable, JoinType.INNER, ProviderTable.aggregator, AggregatorTable.id)
            .selectAll()
            .where { ProviderTable.identity eq identity.value }
            .singleOrNull()
            ?.toProvider()
    }

    override suspend fun findAll(pageable: Pageable): Page<Provider> = dbRead {
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

    override suspend fun addImage(identity: Identity, key: String, url: String) {
        dbTransaction {
            val entity = domainRequireNotNull(
                ProviderEntity.find { ProviderTable.identity eq identity.value }.firstOrNull()
            ) { ProviderNotFoundException() }
            entity.images = entity.images.toMutableMap().apply { put(key, url) }
        }
    }

    override suspend fun deleteByIdentity(identity: Identity) {
        dbTransaction {
            val entity = domainRequireNotNull(
                ProviderEntity.find { ProviderTable.identity eq identity.value }.firstOrNull()
            ) { ProviderNotFoundException() }
            entity.delete()
        }
    }
}
