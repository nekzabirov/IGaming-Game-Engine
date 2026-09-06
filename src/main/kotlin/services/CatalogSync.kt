package services

import clients.GameHub
import db.CasinoGames
import db.CasinoProviders
import db.Platform
import errors.Valid
import org.jetbrains.exposed.sql.batchUpsert
import org.jetbrains.exposed.sql.Coalesce
import org.slf4j.LoggerFactory
import plugins.dbTransaction

/**
 * One call, the whole catalog. The hub already resolved provider matching and deduplication on its
 * side. Every field the hub reports is overwritten on every run; what is ours — `active`, `order`,
 * the bonus flags, blocked countries, `customImages`, `customTags` — is never touched on an existing
 * row, and a new row starts active. Nothing is deleted: a game that dropped out of the feed simply
 * stops being refreshed. `rtp` keeps its last value when the hub reports none (unmeasured, never 0).
 *
 * One upsert per table, no read-back of 11k rows: the DO UPDATE clause names exactly the hub's columns.
 */
class CatalogSync(private val gameHub: GameHub) {

    private val log = LoggerFactory.getLogger(CatalogSync::class.java)

    data class Result(val providers: Int, val games: Int)

    suspend fun run(): Result {
        val catalog = gameHub.listCasino()

        val result = dbTransaction {
            CasinoProviders.batchUpsert(
                catalog.providersList,
                CasinoProviders.identity,
                onUpdate = {
                    it[CasinoProviders.name] = insertValue(CasinoProviders.name)
                    it[CasinoProviders.images] = insertValue(CasinoProviders.images)
                    it[CasinoProviders.tags] = insertValue(CasinoProviders.tags)
                },
            ) { provider ->
                this[CasinoProviders.identity] = Valid.identity(provider.identity)
                this[CasinoProviders.name] = provider.name
                this[CasinoProviders.images] = provider.imagesMap
                this[CasinoProviders.tags] = provider.tagsList
                this[CasinoProviders.active] = true
            }

            val providerIds = CasinoProviders
                .select(CasinoProviders.id, CasinoProviders.identity)
                .associate { it[CasinoProviders.identity] to it[CasinoProviders.id] }

            val games = catalog.gamesList.mapNotNull { game ->
                val providerId = providerIds[game.provider] ?: run {
                    log.warn("Skipping game with unknown provider: game={} provider={}", game.identity, game.provider)
                    return@mapNotNull null
                }
                game to providerId
            }

            CasinoGames.batchUpsert(
                games,
                CasinoGames.identity,
                onUpdate = {
                    it[CasinoGames.name] = insertValue(CasinoGames.name)
                    it[CasinoGames.provider] = insertValue(CasinoGames.provider)
                    it[CasinoGames.tags] = insertValue(CasinoGames.tags)
                    it[CasinoGames.rtp] = Coalesce(insertValue(CasinoGames.rtp), CasinoGames.rtp)
                    it[CasinoGames.freeSpinEnable] = insertValue(CasinoGames.freeSpinEnable)
                    it[CasinoGames.freeChipEnable] = insertValue(CasinoGames.freeChipEnable)
                    it[CasinoGames.jackpotEnable] = insertValue(CasinoGames.jackpotEnable)
                    it[CasinoGames.demoEnable] = insertValue(CasinoGames.demoEnable)
                    it[CasinoGames.bonusBuyEnable] = insertValue(CasinoGames.bonusBuyEnable)
                    it[CasinoGames.locales] = insertValue(CasinoGames.locales)
                    it[CasinoGames.platforms] = insertValue(CasinoGames.platforms)
                    it[CasinoGames.playLines] = insertValue(CasinoGames.playLines)
                    it[CasinoGames.images] = insertValue(CasinoGames.images)
                },
            ) { (game, providerId) ->
                this[CasinoGames.identity] = Valid.identity(game.identity)
                this[CasinoGames.name] = game.name
                this[CasinoGames.provider] = providerId
                this[CasinoGames.tags] = game.tagsList
                // The hub reports `optional` — absent means unmeasured this window, never 0.
                this[CasinoGames.rtp] = if (game.hasRtp()) game.rtp else null
                this[CasinoGames.freeSpinEnable] = game.freespinEnable
                // The hub has no equivalent concept — never populated.
                this[CasinoGames.freeChipEnable] = false
                this[CasinoGames.jackpotEnable] = game.jackpotEnable
                this[CasinoGames.demoEnable] = game.demoEnable
                this[CasinoGames.bonusBuyEnable] = game.bonusBuyEnable
                this[CasinoGames.locales] = game.localesList
                this[CasinoGames.platforms] = game.platformsList.mapNotNull { it.toPlatform() }.map { p -> p.name }
                this[CasinoGames.playLines] = game.playLines
                this[CasinoGames.images] = game.imagesMap
                this[CasinoGames.active] = true
                this[CasinoGames.sortOrder] = 0
            }

            Result(providers = catalog.providersCount, games = games.size)
        }

        log.info("Catalog synced: providers={} games={}", result.providers, result.games)

        return result
    }

    private fun String.toPlatform(): Platform? = when (uppercase()) {
        "DESKTOP" -> Platform.DESKTOP
        "MOBILE" -> Platform.MOBILE
        "DOWNLOAD" -> Platform.DOWNLOAD
        else -> null
    }
}

