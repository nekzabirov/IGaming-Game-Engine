package infrastructure.aggregator.gamehub.adapter

import application.port.external.ICasinoGamePort
import domain.model.CasinoSession
import domain.model.Freespin
import domain.model.Platform
import domain.vo.Currency
import domain.vo.Locale
import gamehub.v1.Common
import gamehub.v1.Gamehub
import infrastructure.aggregator.gamehub.GameHubConfig
import infrastructure.aggregator.gamehub.client.GameHubClient

class GameHubGameAdapter(
    config: GameHubConfig,
) : ICasinoGamePort {

    private val client = GameHubClient(config)

    /**
     * The hub already answers with the games this operator can actually serve — every game whose
     * provider it has a route for — so the feed is taken as-is. `identity` becomes the variant
     * symbol because that is the exact string `OpenGame` expects back.
     */
    override suspend fun getAggregatorGames(): List<ICasinoGamePort.AggregatorGame> =
        client.gameList().map { game ->
            ICasinoGamePort.AggregatorGame(
                symbol = game.identity,
                name = game.name.ifBlank { game.identity },
                providerName = game.provider.name.ifBlank { game.provider.identity },
                freeSpinEnable = game.freespinEnable,
                freeChipEnable = false,
                jackpotEnable = false,
                demoEnable = game.demoEnable,
                bonusBuyEnable = game.bonusBuyEnable,
                locales = game.localesList.filter { it.isNotBlank() }.map { Locale(it) },
                platforms = platforms(game),
                playLines = game.playLines,
                tags = game.tagsList.filter { it.isNotBlank() },
            )
        }

    /** The hub mints no session for a demo and takes no lobby URL — the exit link is the vendor's
     *  own, configured hub-side. */
    override suspend fun getDemoUrl(
        gameSymbol: String,
        locale: Locale,
        platform: Platform,
        currency: Currency,
        lobbyUrl: String,
    ): String = client.openGameDemo(
        game = gameSymbol,
        currency = currency.value,
        lang = locale.value,
        platform = platform,
    )

    /**
     * `session_ref` is our own session token: the hub echoes it back on every Spin and GetBalance
     * of this session, so the inbound wallet service resolves the exact session row rather than
     * inferring it from (player, game).
     *
     * Nothing is returned as `externalToken` — the hub mints no id of its own that we would need to
     * resolve a callback by, and [freespin] is not passed either: a grant is issued ahead of time
     * through `CreateFreespin` and arrives on the wire as `SpinRequest.freespin_id`, never as a
     * launch parameter. [lobbyUrl] has no field in the hub's contract.
     */
    override suspend fun getLaunchUrl(session: CasinoSession, lobbyUrl: String, freespin: Freespin?): ICasinoGamePort.Launch =
        ICasinoGamePort.Launch(
            url = client.openGame(
                game = session.gameVariant.symbol.value,
                playerId = session.playerId.value,
                currency = session.currency.value,
                lang = session.locale.value,
                platform = session.platform,
                sessionRef = session.token.value,
            )
        )

    /** Never empty: `CasinoSessionFactory` refuses a session on a platform the variant does not
     *  list, so a game the hub reports without platforms would be unplayable. */
    private fun platforms(game: Gamehub.Game): List<Platform> =
        game.platformsList.mapNotNull(::platform).distinct().ifEmpty { DEFAULT_PLATFORMS }

    private fun platform(platform: Common.Platform): Platform? = when (platform) {
        Common.Platform.PLATFORM_DESKTOP -> Platform.DESKTOP
        Common.Platform.PLATFORM_MOBILE -> Platform.MOBILE
        else -> null
    }

    private companion object {
        val DEFAULT_PLATFORMS = listOf(Platform.DESKTOP, Platform.MOBILE)
    }
}
