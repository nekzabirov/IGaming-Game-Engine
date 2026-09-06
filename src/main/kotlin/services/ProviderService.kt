package services

import com.nekgamebling.game.v1.CasinoProviderDto
import db.CasinoGameCollections
import db.CasinoGames
import db.CasinoProvider
import db.CasinoProviders
import db.Collections
import db.Page
import db.Pageable
import db.SearchIndexes
import db.searchCanRelax
import db.searchPass
import dto.toDto
import errors.CasinoProviderNotFoundException
import errors.Valid
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.exists
import plugins.dbRead
import plugins.dbTransaction

/** Providers come only from the catalog sync; an operator edits `active`, `order`, blocked countries and the local halves. */
class ProviderService {

    suspend fun save(identity: String, order: Int, active: Boolean, blockedCountry: List<String>) {
        dbTransaction {
            val provider = CasinoProvider.findByIdentity(identity) ?: throw CasinoProviderNotFoundException()
            provider.sortOrder = order
            provider.active = active
            provider.blockedCountry = blockedCountry
        }
    }

    suspend fun find(identity: String): CasinoProviderDto = dbRead {
        CasinoProvider.findByIdentity(identity)?.toDto() ?: throw CasinoProviderNotFoundException()
    }

    suspend fun findAll(
        query: String,
        active: Boolean?,
        inCollections: List<String>,
        inTags: List<String>,
        pageable: Pageable,
    ): Page<CasinoProviderDto> = dbRead {
        fun condition(relaxed: Boolean): Op<Boolean> {
            val conditions = buildList {
                if (query.isNotBlank()) add(SearchIndexes.providers.matches(query, relaxed))
                active?.let { add(Op.build { CasinoProviders.active eq it }) }
                if (inCollections.isNotEmpty()) {
                    add(exists(
                        CasinoGames
                            .join(CasinoGameCollections, JoinType.INNER, CasinoGames.id, CasinoGameCollections.game)
                            .select(CasinoGames.provider)
                            .where {
                                (CasinoGames.provider eq CasinoProviders.id) and
                                    (CasinoGameCollections.collection inSubQuery Collections.select(Collections.id)
                                        .where { Collections.identity inList inCollections })
                            },
                    ))
                }
                if (inTags.isNotEmpty()) add(CasinoProviders.tags.hasAnyTag(inTags) or CasinoProviders.customTags.hasAnyTag(inTags))
            }
            return conditions.reduceOrNull { acc, op -> acc and op } ?: Op.TRUE
        }

        val pass = searchPass(
            relaxable = searchCanRelax(query),
            condition = ::condition,
            count = { condition -> CasinoProvider.find { condition }.count() },
        )

        val items = CasinoProvider.find { pass.condition }
            .orderBy(*SearchIndexes.providers.relevanceOrdering(query), CasinoProviders.sortOrder to SortOrder.ASC)
            .limit(pageable.sizeReal)
            .offset(pageable.offset)
            .map { it.toDto() }

        pageable.page(items, pass.totalItems)
    }

    /** Distinct tags across ACTIVE providers (hub's and local), alphabetical. */
    suspend fun tags(pageable: Pageable): Page<String> = dbRead {
        CasinoProviders
            .select(CasinoProviders.tags, CasinoProviders.customTags)
            .where { CasinoProviders.active eq true }
            .flatMap { it[CasinoProviders.tags] + it[CasinoProviders.customTags] }
            .distinct()
            .sorted()
            .slice(pageable)
    }

    suspend fun batch(identities: List<String>): List<CasinoProviderDto> = dbRead {
        CasinoProvider.find { CasinoProviders.identity inList identities }
            .orderBy(CasinoProviders.sortOrder to SortOrder.ASC)
            .map { it.toDto() }
    }

    suspend fun updateImage(identity: String, key: String, url: String) {
        Valid.imageUrl(url)
        dbTransaction {
            val provider = CasinoProvider.findByIdentity(identity) ?: throw CasinoProviderNotFoundException()
            provider.customImages = provider.customImages + (key to url)
        }
    }

    suspend fun updateTags(identity: String, tags: List<String>) {
        dbTransaction {
            val provider = CasinoProvider.findByIdentity(identity) ?: throw CasinoProviderNotFoundException()
            provider.customTags = tags.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        }
    }
}
