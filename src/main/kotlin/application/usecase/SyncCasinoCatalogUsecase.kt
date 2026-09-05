package application.usecase

import domain.model.CasinoGame
import domain.model.CasinoProvider
import domain.model.Platform
import domain.repository.ICasinoGameRepository
import domain.repository.ICasinoProviderRepository
import domain.vo.Identity
import domain.vo.ImageMap
import domain.vo.Locale
import gamehub.v1.Gateway
import infrastructure.gamehub.GameHubClient
import org.slf4j.LoggerFactory

/**
 * One call, the whole catalog: the hub already resolved provider matching and deduplication on
 * its own side (`docs/` — there is no fuzzy matcher here any more). Every field the hub reports
 * (`images`, `tags`, every former-variant field, `rtp`) is overwritten on every run; `customImages`
 * and `customTags` are LOCAL overrides the sync never touches. Nothing is deleted — a game that
 * dropped out of the feed simply stops being refreshed.
 */
class SyncCasinoCatalogUsecase(
    private val gameHubClient: GameHubClient,
    private val gameRepository: ICasinoGameRepository,
    private val providerRepository: ICasinoProviderRepository,
) {

    private val logger = LoggerFactory.getLogger(SyncCasinoCatalogUsecase::class.java)

    suspend operator fun invoke(): Result<Response> = runCatching {
        val catalog = gameHubClient.listCasino()

        val existingProviders = providerRepository.findAll().associateBy { it.identity.value }
        val providers = catalog.providersList.map { it.toDomain(existingProviders[it.identity]) }
        providerRepository.saveAll(providers)
        val providerByIdentity = providers.associateBy { it.identity.value }

        val existingGames = gameRepository.findAll().associateBy { it.identity.value }
        val games = catalog.gamesList.mapNotNull { game ->
            val provider = providerByIdentity[game.provider] ?: run {
                logger.warn("Skipping game with unknown provider: game={} provider={}", game.identity, game.provider)
                return@mapNotNull null
            }
            game.toDomain(provider, existingGames[game.identity])
        }
        gameRepository.saveAll(games)

        logger.info("Catalog synced: providers={} games={}", providers.size, games.size)

        Response(providers = providers.size, games = games.size)
    }.onFailure { e ->
        logger.error("Catalog sync failed", e)
    }

    private fun Gateway.Provider.toDomain(existing: CasinoProvider?): CasinoProvider = CasinoProvider(
        identity = Identity(identity),
        name = name,
        images = ImageMap(imagesMap),
        customImages = existing?.customImages ?: ImageMap.EMPTY,
        order = existing?.order ?: 100,
        active = existing?.active ?: true,
        blockedCountry = existing?.blockedCountry ?: emptyList(),
        tags = tagsList,
        customTags = existing?.customTags ?: emptyList(),
    )

    private fun Gateway.Game.toDomain(provider: CasinoProvider, existing: CasinoGame?): CasinoGame = CasinoGame(
        identity = Identity(identity),
        name = name,
        provider = provider,
        collections = existing?.collections ?: emptyList(),
        bonusBetEnable = existing?.bonusBetEnable ?: true,
        bonusWageringEnable = existing?.bonusWageringEnable ?: true,
        tags = tagsList,
        // Local editorial tags are ours alone: `tags` above is overwritten wholesale every run, so
        // anything curated here (lobby rails, promos) would not survive there.
        customTags = existing?.customTags ?: emptyList(),
        // The hub reports `optional` — absent means unmeasured this window, never 0. Keep the
        // last known value rather than blanking it out.
        rtp = if (hasRtp()) rtp else existing?.rtp,
        freeSpinEnable = freespinEnable,
        // The hub has no equivalent concept — never populated, no data source for it.
        freeChipEnable = false,
        jackpotEnable = jackpotEnable,
        demoEnable = demoEnable,
        bonusBuyEnable = bonusBuyEnable,
        locales = localesList.map { Locale(it) },
        platforms = platformsList.mapNotNull { it.toPlatform() },
        playLines = playLines,
        active = existing?.active ?: true,
        images = ImageMap(imagesMap),
        customImages = existing?.customImages ?: ImageMap.EMPTY,
        order = existing?.order ?: 0,
    )

    private fun String.toPlatform(): Platform? = when (uppercase()) {
        "DESKTOP" -> Platform.DESKTOP
        "MOBILE" -> Platform.MOBILE
        "DOWNLOAD" -> Platform.DOWNLOAD
        else -> null
    }

    data class Response(val providers: Int, val games: Int)
}
