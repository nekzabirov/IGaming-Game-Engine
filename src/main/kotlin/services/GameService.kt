package services

import com.nekgamebling.game.v1.BatchCasinoGameQuery
import com.nekgamebling.game.v1.BatchCasinoGameQueryKt
import com.nekgamebling.game.v1.CasinoGameFilter
import com.nekgamebling.game.v1.CasinoGamePageDto
import com.nekgamebling.game.v1.FindCasinoGameQuery
import com.nekgamebling.game.v1.FindCasinoGameQueryKt
import db.CasinoGame
import db.CasinoGameFavourites
import db.CasinoGames
import db.CasinoRounds
import db.Page
import db.Pageable
import db.searchPass
import dto.toDto
import dto.toPageDto
import errors.CasinoGameNotFoundException
import errors.Valid
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.with
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.max
import plugins.dbRead
import plugins.dbTransaction

/** RTP bucket relative to [CasinoGame.DEFAULT_RTP]. */
enum class RtpType { HOT, COLD }

/**
 * The game catalog. `name`, `tags`, `images`, `rtp` and every capability flag are GameHub's and
 * written only by [CatalogSync]; what an operator edits here is `active`, `order`, the two bonus
 * flags, and the LOCAL halves — `customImages` / `customTags` — that the sync never touches.
 */
class GameService {

    suspend fun save(identity: String, bonusBetEnable: Boolean, bonusWageringEnable: Boolean, active: Boolean, order: Int) {
        dbTransaction {
            val game = CasinoGame.findByIdentity(identity) ?: throw CasinoGameNotFoundException()
            game.bonusBetEnable = bonusBetEnable
            game.bonusWageringEnable = bonusWageringEnable
            game.active = active
            game.sortOrder = order
        }
    }

    suspend fun find(identity: String): FindCasinoGameQuery.Result = dbRead {
        val game = CasinoGame.find { CasinoGames.identity eq identity }
            .with(CasinoGame::provider, CasinoGame::collections)
            .firstOrNull() ?: throw CasinoGameNotFoundException()

        FindCasinoGameQueryKt.result {
            item = game.toDto()
            provider = game.provider.toDto()
            collections.addAll(game.collections.map { it.toDto() })
        }
    }

    suspend fun findAll(filter: CasinoGameFilter, pageable: Pageable): CasinoGamePageDto = dbRead {
        val pass = searchPass(
            relaxable = filter.isRelaxable(),
            condition = { relaxed -> filter.toCondition(relaxed) },
            count = { condition -> CasinoGame.find { condition }.count() },
        )

        CasinoGame.find { pass.condition }
            .orderBy(*filter.toOrdering())
            .limit(pageable.sizeReal)
            .offset(pageable.offset)
            .with(CasinoGame::provider, CasinoGame::collections)
            .toList()
            .toPageDto(pass.totalItems)
    }

    /** ACTIVE games above (HOT, rtp DESC) or below (COLD, rtp ASC) the default; unmeasured = neither. */
    suspend fun findAllActiveRtp(type: RtpType, filter: CasinoGameFilter, pageable: Pageable): CasinoGamePageDto = dbRead {
        val rtpCondition = when (type) {
            RtpType.HOT -> Op.build { CasinoGames.rtp greater CasinoGame.DEFAULT_RTP }
            RtpType.COLD -> Op.build { CasinoGames.rtp less CasinoGame.DEFAULT_RTP }
        }
        val rtpOrder = when (type) {
            RtpType.HOT -> SortOrder.DESC
            RtpType.COLD -> SortOrder.ASC
        }

        val pass = searchPass(
            relaxable = filter.isRelaxable(),
            condition = { relaxed -> filter.toCondition(relaxed) and Op.build { CasinoGames.active eq true } and rtpCondition },
            count = { condition -> CasinoGame.find { condition }.count() },
        )

        CasinoGame.find { pass.condition }
            .orderBy(CasinoGames.rtp to rtpOrder, CasinoGames.sortOrder to SortOrder.ASC, CasinoGames.id to SortOrder.ASC)
            .limit(pageable.sizeReal)
            .offset(pageable.offset)
            .with(CasinoGame::provider, CasinoGame::collections)
            .toList()
            .toPageDto(pass.totalItems)
    }

    /** Distinct tags across ACTIVE games (hub's and local), alphabetical, paged in memory. */
    suspend fun tags(pageable: Pageable): Page<String> = dbRead {
        val tags = CasinoGames
            .select(CasinoGames.tags, CasinoGames.customTags)
            .where { CasinoGames.active eq true }
            .flatMap { it[CasinoGames.tags] + it[CasinoGames.customTags] }
            .distinct()
            .sorted()

        tags.slice(pageable)
    }

    suspend fun batch(identities: List<String>): BatchCasinoGameQuery.Result = dbRead {
        val games = CasinoGame.find { CasinoGames.identity inList identities }
            .orderBy(CasinoGames.sortOrder to SortOrder.ASC)
            .with(CasinoGame::provider, CasinoGame::collections)
            .toList()

        BatchCasinoGameQueryKt.result {
            items.addAll(games.map { it.toDto() })
            providers.addAll(games.map { it.provider }.distinctBy { it.id }.map { it.toDto() })
            collections.addAll(games.flatMap { it.collections }.distinctBy { it.id }.map { it.toDto() })
        }
    }

    /** A LOCAL override on top of the hub's artwork, one key at a time. */
    suspend fun updateImage(identity: String, key: String, url: String) {
        Valid.imageUrl(url)
        dbTransaction {
            val game = CasinoGame.findByIdentity(identity) ?: throw CasinoGameNotFoundException()
            game.customImages = game.customImages + (key to url)
        }
    }

    /** Replaces the LOCAL tag list whole; the synced `tags` stay the hub's. */
    suspend fun updateTags(identity: String, tags: List<String>) {
        dbTransaction {
            val game = CasinoGame.findByIdentity(identity) ?: throw CasinoGameNotFoundException()
            game.customTags = tags.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        }
    }

    suspend fun addFavourite(identity: String, playerId: String) {
        dbTransaction {
            val gameId = gameId(identity)
            CasinoGameFavourites.insertIgnore {
                it[game] = gameId
                it[CasinoGameFavourites.playerId] = playerId
            }
        }
    }

    suspend fun removeFavourite(identity: String, playerId: String) {
        dbTransaction {
            val gameId = gameId(identity)
            CasinoGameFavourites.deleteWhere { (game eq gameId) and (CasinoGameFavourites.playerId eq playerId) }
        }
    }

    suspend fun favourites(playerId: String, filter: CasinoGameFilter, pageable: Pageable): CasinoGamePageDto = dbRead {
        fun favourites(condition: Op<Boolean>) = (CasinoGameFavourites innerJoin CasinoGames)
            .select(CasinoGames.id, CasinoGameFavourites.id)
            .where { (CasinoGameFavourites.playerId eq playerId) and condition }

        val pass = searchPass(
            relaxable = filter.isRelaxable(),
            condition = { relaxed -> filter.toCondition(relaxed) },
            count = { condition -> favourites(condition).count() },
        )

        val gameIds = favourites(pass.condition)
            .orderBy(*filter.relevanceOrdering(), CasinoGameFavourites.id to SortOrder.DESC)
            .limit(pageable.sizeReal)
            .offset(pageable.offset)
            .map { it[CasinoGames.id] }

        loadInOrder(gameIds).toPageDto(pass.totalItems)
    }

    /** The games a player recently played, one row per game, most recent round first. */
    suspend fun lastPlayed(playerId: String, pageable: Pageable): CasinoGamePageDto = dbRead {
        // The round's own PK is the recency order — a sportsbook leg (game == null) is excluded.
        val lastRoundId = CasinoRounds.id.max()

        val rounds = CasinoRounds
            .select(CasinoRounds.game, lastRoundId)
            .where { (CasinoRounds.playerId eq playerId) and CasinoRounds.game.isNotNull() }
            .groupBy(CasinoRounds.game)

        val totalItems = rounds.count()

        val gameIds = rounds
            .orderBy(lastRoundId to SortOrder.DESC)
            .limit(pageable.sizeReal)
            .offset(pageable.offset)
            .mapNotNull { it[CasinoRounds.game] }

        loadInOrder(gameIds).toPageDto(totalItems)
    }

    private fun gameId(identity: String) = CasinoGames
        .select(CasinoGames.id)
        .where { CasinoGames.identity eq identity }
        .singleOrNull()
        ?.get(CasinoGames.id)
        ?: throw CasinoGameNotFoundException()
}

/** Loads a page of games by id and hands them back in the order the paging query chose. */
internal fun loadInOrder(gameIds: List<EntityID<Long>>): List<CasinoGame> {
    val byId = CasinoGame.forEntityIds(gameIds)
        .with(CasinoGame::provider, CasinoGame::collections)
        .associateBy { it.id }

    return gameIds.mapNotNull { byId[it] }
}

/** In-memory paging for small, already materialized lists (tag directories). */
internal fun <T> List<T>.slice(pageable: Pageable): Page<T> {
    val from = pageable.offset.toInt().coerceAtMost(size)
    val to = (from + pageable.sizeReal).coerceAtMost(size)
    return pageable.page(subList(from, to), size.toLong())
}
