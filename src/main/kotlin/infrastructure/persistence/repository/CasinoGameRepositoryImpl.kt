package infrastructure.persistence.repository

import domain.repository.ICasinoGameRepository
import domain.exception.domainRequireNotNull
import domain.exception.notfound.CasinoGameNotFoundException
import domain.exception.notfound.CasinoProviderNotFoundException
import domain.model.CasinoGame
import domain.vo.Identity
import domain.vo.Page
import domain.vo.Pageable
import infrastructure.persistence.dbRead
import infrastructure.persistence.dbTransaction
import infrastructure.persistence.entity.CasinoGameEntity
import infrastructure.persistence.entity.CasinoProviderEntity
import infrastructure.persistence.mapper.CasinoGameMapper.toDomain
import infrastructure.persistence.table.CasinoGameTable
import infrastructure.persistence.table.CasinoProviderTable
import org.jetbrains.exposed.dao.with
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.batchUpsert

class CasinoGameRepositoryImpl : ICasinoGameRepository {

    private val gameChain = arrayOf(
        CasinoGameEntity::provider,
        CasinoGameEntity::collections,
    )

    override suspend fun save(game: CasinoGame): CasinoGame = dbTransaction {
        val providerEntity = domainRequireNotNull(
            CasinoProviderEntity.find { CasinoProviderTable.identity eq game.provider.identity.value }.firstOrNull()
        ) { CasinoProviderNotFoundException() }

        val gameEntity = CasinoGameEntity.find { CasinoGameTable.identity eq game.identity.value }.firstOrNull()

        if (gameEntity != null) {
            gameEntity.apply {
                name = game.name
                provider = providerEntity
                bonusBetEnable = game.bonusBetEnable
                bonusWageringEnable = game.bonusWageringEnable
                tags = game.tags
                rtp = game.rtp
                freeSpinEnable = game.freeSpinEnable
                freeChipEnable = game.freeChipEnable
                jackpotEnable = game.jackpotEnable
                demoEnable = game.demoEnable
                bonusBuyEnable = game.bonusBuyEnable
                locales = game.locales.map { it.value }
                platforms = game.platforms.map { it.name }
                playLines = game.playLines
                active = game.active
                images = game.images.data
                customImages = game.customImages.data
                sortOrder = game.order
            }
        } else {
            CasinoGameEntity.new {
                identity = game.identity.value
                name = game.name
                provider = providerEntity
                bonusBetEnable = game.bonusBetEnable
                bonusWageringEnable = game.bonusWageringEnable
                tags = game.tags
                rtp = game.rtp
                freeSpinEnable = game.freeSpinEnable
                freeChipEnable = game.freeChipEnable
                jackpotEnable = game.jackpotEnable
                demoEnable = game.demoEnable
                bonusBuyEnable = game.bonusBuyEnable
                locales = game.locales.map { it.value }
                platforms = game.platforms.map { it.name }
                playLines = game.playLines
                active = game.active
                images = game.images.data
                customImages = game.customImages.data
                sortOrder = game.order
            }
        }

        // Collection membership is owned exclusively by the three CollectionService
        // game-membership RPCs (AddCasinoGame / RemoveCasinoGame / UpdateCasinoGameOrder). Save never
        // touches CasinoGameCollectionTable, so per-collection sort order is never
        // clobbered by a routine game upsert.

        game
    }

    override suspend fun saveAll(gameList: List<CasinoGame>): List<CasinoGame> = dbTransaction {
        val providerIdentities = gameList.map { it.provider.identity.value }.distinct()
        val providerMap = CasinoProviderEntity.find { CasinoProviderTable.identity inList providerIdentities }
            .associateBy { it.identity }

        CasinoGameTable.batchUpsert(gameList, keys = arrayOf(CasinoGameTable.identity)) { game ->
            val providerEntity = domainRequireNotNull(providerMap[game.provider.identity.value]) {
                CasinoProviderNotFoundException()
            }

            this[CasinoGameTable.identity] = game.identity.value
            this[CasinoGameTable.name] = game.name
            this[CasinoGameTable.provider] = providerEntity.id
            this[CasinoGameTable.bonusBetEnable] = game.bonusBetEnable
            this[CasinoGameTable.bonusWageringEnable] = game.bonusWageringEnable
            this[CasinoGameTable.tags] = game.tags
            this[CasinoGameTable.rtp] = game.rtp
            this[CasinoGameTable.freeSpinEnable] = game.freeSpinEnable
            this[CasinoGameTable.freeChipEnable] = game.freeChipEnable
            this[CasinoGameTable.jackpotEnable] = game.jackpotEnable
            this[CasinoGameTable.demoEnable] = game.demoEnable
            this[CasinoGameTable.bonusBuyEnable] = game.bonusBuyEnable
            this[CasinoGameTable.locales] = game.locales.map { it.value }
            this[CasinoGameTable.platforms] = game.platforms.map { it.name }
            this[CasinoGameTable.playLines] = game.playLines
            this[CasinoGameTable.active] = game.active
            this[CasinoGameTable.images] = game.images.data
            this[CasinoGameTable.customImages] = game.customImages.data
            this[CasinoGameTable.sortOrder] = game.order
        }

        gameList
    }

    override suspend fun findByIdentity(identity: Identity): CasinoGame? = dbRead {
        CasinoGameEntity.find { CasinoGameTable.identity eq identity.value }
            .with(*gameChain)
            .firstOrNull()?.toDomain()
    }

    override suspend fun findAll(): List<CasinoGame> = dbRead {
        CasinoGameEntity.all()
            .with(*gameChain)
            .toList()
            .map { it.toDomain() }
    }

    override suspend fun findAll(pageable: Pageable): Page<CasinoGame> = dbRead {
        val totalItems = CasinoGameEntity.count()

        val items = CasinoGameEntity.all()
            .orderBy(CasinoGameTable.sortOrder to SortOrder.ASC)
            .limit(pageable.sizeReal)
            .offset(pageable.offset)
            .with(*gameChain)
            .toList()
            .map { it.toDomain() }

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
                CasinoGameEntity.find { CasinoGameTable.identity eq identity.value }.firstOrNull()
            ) { CasinoGameNotFoundException() }
            entity.customImages = entity.customImages.toMutableMap().apply { put(key, url) }
        }
    }
}
