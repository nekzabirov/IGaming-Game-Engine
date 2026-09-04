package infrastructure.persistence.repository

import domain.repository.ICasinoProviderRepository
import domain.exception.domainRequireNotNull
import domain.exception.notfound.CasinoProviderNotFoundException
import domain.model.CasinoProvider
import domain.vo.Identity
import domain.vo.Page
import domain.vo.Pageable
import infrastructure.persistence.dbRead
import infrastructure.persistence.dbTransaction
import infrastructure.persistence.entity.CasinoProviderEntity
import infrastructure.persistence.mapper.CasinoProviderMapper.toCasinoProvider
import infrastructure.persistence.table.CasinoProviderTable
import org.jetbrains.exposed.sql.batchUpsert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.upsert

class CasinoProviderRepositoryImpl : ICasinoProviderRepository {

    override suspend fun save(provider: CasinoProvider): CasinoProvider = dbTransaction {
        CasinoProviderTable.upsert(keys = arrayOf(CasinoProviderTable.identity)) {
            it[identity] = provider.identity.value
            it[name] = provider.name
            it[images] = provider.images.data
            it[customImages] = provider.customImages.data
            it[sortOrder] = provider.order
            it[active] = provider.active
            it[blockedCountry] = provider.blockedCountry.map { it.value }
            it[tags] = provider.tags
        }

        provider
    }

    override suspend fun saveAll(providers: List<CasinoProvider>): List<CasinoProvider> = dbTransaction {
        CasinoProviderTable.batchUpsert(providers, keys = arrayOf(CasinoProviderTable.identity)) { provider ->
            this[CasinoProviderTable.identity] = provider.identity.value
            this[CasinoProviderTable.name] = provider.name
            this[CasinoProviderTable.images] = provider.images.data
            this[CasinoProviderTable.customImages] = provider.customImages.data
            this[CasinoProviderTable.sortOrder] = provider.order
            this[CasinoProviderTable.active] = provider.active
            this[CasinoProviderTable.blockedCountry] = provider.blockedCountry.map { it.value }
            this[CasinoProviderTable.tags] = provider.tags
        }

        providers
    }

    override suspend fun findAll(): List<CasinoProvider> = dbRead {
        CasinoProviderTable.selectAll().map { it.toCasinoProvider() }
    }

    override suspend fun findByIdentity(identity: Identity): CasinoProvider? = dbRead {
        CasinoProviderTable
            .selectAll()
            .where { CasinoProviderTable.identity eq identity.value }
            .singleOrNull()
            ?.toCasinoProvider()
    }

    override suspend fun findAll(pageable: Pageable): Page<CasinoProvider> = dbRead {
        val totalItems = CasinoProviderTable.selectAll().count()
        val items = CasinoProviderTable
            .selectAll()
            .limit(pageable.sizeReal, pageable.offset)
            .map { it.toCasinoProvider() }

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
                CasinoProviderEntity.find { CasinoProviderTable.identity eq identity.value }.firstOrNull()
            ) { CasinoProviderNotFoundException() }
            entity.customImages = entity.customImages.toMutableMap().apply { put(key, url) }
        }
    }
}
