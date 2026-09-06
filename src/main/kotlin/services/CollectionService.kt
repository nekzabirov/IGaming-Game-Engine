package services

import com.nekgamebling.game.v1.CasinoGameFilter
import com.nekgamebling.game.v1.CasinoGamePageDto
import com.nekgamebling.game.v1.CollectionDto
import db.CasinoGameCollections
import db.CasinoGames
import db.CasinoProviders
import db.Collection
import db.Collections
import db.Page
import db.Pageable
import db.SearchIndexes
import db.searchCanRelax
import db.searchPass
import dto.toDto
import dto.toPageDto
import errors.CasinoGameNotFoundException
import errors.CollectionNotFoundException
import errors.Valid
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.exists
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.max
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import plugins.dbRead
import plugins.dbTransaction

/** Lobby rails: fully operator-owned, including which games are in and in what order. */
class CollectionService {

    suspend fun save(identity: String, name: Map<String, String>, tags: List<String>, active: Boolean, order: Int) {
        dbTransaction {
            val existing = Collection.findByIdentity(identity)
            if (existing != null) {
                existing.name = name
                existing.tags = tags
                existing.active = active
                existing.sortOrder = order
            } else {
                Collection.new {
                    this.identity = identity
                    this.name = name
                    this.tags = tags
                    this.images = emptyMap()
                    this.active = active
                    this.sortOrder = order
                }
            }
        }
    }

    suspend fun find(identity: String): CollectionDto = dbRead {
        Collection.findByIdentity(identity)?.toDto() ?: throw CollectionNotFoundException()
    }

    suspend fun findAll(
        query: String,
        active: Boolean?,
        inTags: List<String>,
        inProviders: List<String>,
        pageable: Pageable,
    ): Page<CollectionDto> = dbRead {
        fun condition(relaxed: Boolean): Op<Boolean> {
            val conditions = buildList {
                if (query.isNotBlank()) add(SearchIndexes.collections.matches(query, relaxed))
                active?.let { add(Op.build { Collections.active eq it }) }
                // The collection's OWN tags, ANY-of.
                if (inTags.isNotEmpty()) add(Collections.tags.hasAnyTag(inTags))
                // Collections that CONTAIN at least one game of any of these providers.
                if (inProviders.isNotEmpty()) {
                    add(exists(
                        CasinoGameCollections
                            .join(CasinoGames, JoinType.INNER, CasinoGameCollections.game, CasinoGames.id)
                            .select(CasinoGameCollections.collection)
                            .where {
                                (CasinoGameCollections.collection eq Collections.id) and
                                    (CasinoGames.provider inSubQuery CasinoProviders.select(CasinoProviders.id)
                                        .where { CasinoProviders.identity inList inProviders })
                            },
                    ))
                }
            }
            return conditions.reduceOrNull { acc, op -> acc and op } ?: Op.TRUE
        }

        val pass = searchPass(
            relaxable = searchCanRelax(query),
            condition = ::condition,
            count = { condition -> Collection.find { condition }.count() },
        )

        val items = Collection.find { pass.condition }
            .orderBy(*SearchIndexes.collections.relevanceOrdering(query), Collections.sortOrder to SortOrder.ASC)
            .limit(pageable.sizeReal)
            .offset(pageable.offset)
            .map { it.toDto() }

        pageable.page(items, pass.totalItems)
    }

    suspend fun batch(identities: List<String>): List<CollectionDto> = dbRead {
        Collection.find { Collections.identity inList identities }
            .orderBy(Collections.sortOrder to SortOrder.ASC)
            .map { it.toDto() }
    }

    /** The rail itself: the collection's games in their curated position, filtered like any listing. */
    suspend fun games(identity: String, filter: CasinoGameFilter, pageable: Pageable): CasinoGamePageDto = dbRead {
        fun rail(condition: Op<Boolean>) = (CasinoGameCollections innerJoin CasinoGames innerJoin Collections)
            .select(CasinoGames.id, CasinoGameCollections.sortOrder)
            .where { (Collections.identity eq identity) and condition }

        val pass = searchPass(
            relaxable = filter.isRelaxable(),
            condition = { relaxed -> filter.toCondition(relaxed) },
            count = { condition -> rail(condition).count() },
        )

        val gameIds = rail(pass.condition)
            .orderBy(*filter.relevanceOrdering(), CasinoGameCollections.sortOrder to SortOrder.ASC, CasinoGames.id to SortOrder.ASC)
            .limit(pageable.sizeReal)
            .offset(pageable.offset)
            .map { it[CasinoGames.id] }

        loadInOrder(gameIds).toPageDto(pass.totalItems)
    }

    /** Idempotent; a new member lands at the end (`max(sort_order) + 1`, or 0 when empty). */
    suspend fun addGame(identity: String, gameIdentity: String) {
        dbTransaction {
            val collectionId = collectionId(identity)
            val gameId = gameId(gameIdentity)

            val alreadyMember = CasinoGameCollections
                .selectAll()
                .where { (CasinoGameCollections.collection eq collectionId) and (CasinoGameCollections.game eq gameId) }
                .any()
            if (alreadyMember) return@dbTransaction

            val maxOrder = CasinoGameCollections.sortOrder.max()
            val currentMax = CasinoGameCollections
                .select(maxOrder)
                .where { CasinoGameCollections.collection eq collectionId }
                .singleOrNull()
                ?.get(maxOrder)

            CasinoGameCollections.insert {
                it[collection] = collectionId
                it[game] = gameId
                it[sortOrder] = if (currentMax == null) 0 else currentMax + 1
            }
        }
    }

    /** Idempotent; the remaining members keep their positions (holes are fine for ORDER BY). */
    suspend fun removeGame(identity: String, gameIdentity: String) {
        dbTransaction {
            val collectionId = collectionId(identity)
            val gameId = gameId(gameIdentity)
            CasinoGameCollections.deleteWhere { (collection eq collectionId) and (game eq gameId) }
        }
    }

    suspend fun updateGameOrder(identity: String, gameIdentity: String, order: Int) {
        dbTransaction {
            val collectionId = collectionId(identity)
            val gameId = gameId(gameIdentity)

            val affected = CasinoGameCollections.update({
                (CasinoGameCollections.collection eq collectionId) and (CasinoGameCollections.game eq gameId)
            }) {
                it[sortOrder] = order
            }

            // Zero rows touched: the game is not a member of this collection.
            if (affected == 0) throw CasinoGameNotFoundException()
        }
    }

    suspend fun updateImage(identity: String, key: String, url: String) {
        Valid.imageUrl(url)
        dbTransaction {
            val collection = Collection.findByIdentity(identity) ?: throw CollectionNotFoundException()
            collection.images = collection.images + (key to url)
        }
    }

    /** Drops the rail and its memberships; the games on the other side survive. */
    suspend fun delete(identity: String) {
        dbTransaction {
            val collectionId = collectionId(identity)
            CasinoGameCollections.deleteWhere { collection eq collectionId }
            Collections.deleteWhere { id eq collectionId }
        }
    }

    private fun collectionId(identity: String) = Collections
        .select(Collections.id)
        .where { Collections.identity eq identity }
        .singleOrNull()
        ?.get(Collections.id)
        ?: throw CollectionNotFoundException()

    private fun gameId(identity: String) = CasinoGames
        .select(CasinoGames.id)
        .where { CasinoGames.identity eq identity }
        .singleOrNull()
        ?.get(CasinoGames.id)
        ?: throw CasinoGameNotFoundException()
}
