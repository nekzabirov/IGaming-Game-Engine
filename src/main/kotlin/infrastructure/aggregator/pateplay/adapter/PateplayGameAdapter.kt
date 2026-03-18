package infrastructure.aggregator.pateplay.adapter

import application.port.external.IGamePort
import domain.model.Platform
import domain.model.Session
import domain.vo.Currency
import domain.vo.Locale
import infrastructure.aggregator.pateplay.PateplayConfig
import io.ktor.http.URLBuilder

class PateplayGameAdapter(
    private val config: PateplayConfig,
) : IGamePort {

    override suspend fun getAggregatorGames(): List<IGamePort.AggregatorGame> {
        return STATIC_GAMES.map { game ->
            IGamePort.AggregatorGame(
                symbol = game.symbol,
                name = game.name,
                providerName = PROVIDER_NAME,
                freeSpinEnable = game.freeSpinEnable,
                freeChipEnable = false,
                jackpotEnable = false,
                demoEnable = game.demoEnable,
                bonusBuyEnable = false,
                locales = SUPPORTED_LOCALES,
                platforms = listOf(Platform.DESKTOP, Platform.MOBILE)
            )
        }
    }

    override suspend fun getDemoUrl(
        gameSymbol: String,
        locale: Locale,
        platform: Platform,
        currency: Currency,
        lobbyUrl: String,
    ): String {
        check(config.gameDemoLaunchUrl.isNotBlank()) { "PatePlay demo launch URL not configured" }

        return buildLaunchUrl(
            baseHost = config.gameDemoLaunchUrl,
            gameSymbol = gameSymbol,
            sessionToken = "",
            playerId = "",
            locale = locale,
            platform = platform
        )
    }

    override suspend fun getLunchUrl(session: Session, lobbyUrl: String): String {
        check(config.gameLaunchUrl.isNotBlank()) { "PatePlay game launch URL not configured" }

        return buildLaunchUrl(
            baseHost = config.gameLaunchUrl,
            gameSymbol = session.gameVariant.symbol,
            sessionToken = session.token,
            playerId = session.playerId.value,
            locale = session.locale,
            platform = session.platform
        )
    }

    private fun buildLaunchUrl(
        baseHost: String,
        gameSymbol: String,
        sessionToken: String,
        playerId: String,
        locale: Locale,
        platform: Platform
    ): String {
        return URLBuilder("https://$baseHost").apply {
            parameters.append("siteCode", config.siteCode)
            parameters.append("authCode", sessionToken)
            parameters.append("playerId", playerId)
            parameters.append("language", locale.value)
            parameters.append("device", platform.toPateplayDevice())
            parameters.append("game", gameSymbol)
        }.buildString()
    }

    private fun Platform.toPateplayDevice(): String = when (this) {
        Platform.DESKTOP -> "desktop"
        Platform.MOBILE -> "mobile"
        Platform.DOWNLOAD -> "web"
    }

    companion object {
        private const val PROVIDER_NAME = "PatePlay"

        private val SUPPORTED_LOCALES = listOf(
            "pp", "af", "am", "ar", "arn", "as", "az", "ba", "be", "bg", "bn", "bo", "br",
            "bs", "ca", "co", "cs", "cy", "da", "de", "dsb", "dv", "el", "en", "es", "et",
            "eu", "fa", "fi", "fil", "fo", "fr", "fy", "ga", "gd", "gl", "gsw", "gu", "ha",
            "he", "hi", "hr", "hsb", "hu", "hy", "id", "ig", "ii", "is", "it", "iu", "ja",
            "ka", "kk", "kl", "km", "kn", "ko", "kok", "ky", "kb", "lo", "lt", "lv", "mi",
            "mk", "ml", "mn", "moh", "mr", "ms", "mt", "my", "nb", "ne", "nl", "nn", "no",
            "nso", "oc", "or", "pa", "pl", "prs", "ps", "pt", "quc", "quz", "rm", "ro", "ru",
            "rw", "sa", "sah", "se", "si", "sk", "sl", "sma", "smj", "smn", "sms", "sq", "sr",
            "sv", "sw", "syr", "ta", "te", "tg", "th", "tk", "tn", "tr", "tt", "tzm", "ug",
            "uk", "ur", "uz", "vi", "wo", "xh", "yo", "zh", "zu"
        ).map { Locale(it) }

        private val STATIC_GAMES = listOf(
            StaticGame("regal-spins-5", "Regal Spins 5"),
            StaticGame("regal-spins-10", "Regal Spins 10"),
            StaticGame("regal-spins-20", "Regal Spins 20"),
            StaticGame("fruit-chase-5", "Fruit Chase 5"),
            StaticGame("fruit-chase-10", "Fruit Chase 10"),
            StaticGame("fruit-chase-20", "Fruit Chase 20"),
            StaticGame("sevens-heat-5", "Sevens Heat 5"),
            StaticGame("sevens-heat-10", "Sevens Heat 10"),
            StaticGame("sevens-heat-20", "Sevens Heat 20"),
            StaticGame("sevens-heat-40", "Sevens Heat 40"),
            StaticGame("sevens-heat-100", "Sevens Heat 100"),
            StaticGame("lady-chance-5", "Lady Chance 5"),
            StaticGame("lady-chance-10", "Lady Chance 10"),
            StaticGame("lady-chance-20", "Lady Chance 20"),
            StaticGame("lady-chance-40", "Lady Chance 40"),
            StaticGame("scorching-reels-40", "Scorching Reels 40"),
            StaticGame("scorching-reels-100", "Scorching Reels 100"),
            StaticGame("redfate-5", "Red Fate 5"),
            StaticGame("redfate-10", "Red Fate 10"),
            StaticGame("redfate-20", "Red Fate 20"),
            StaticGame("redfate-40", "Red Fate 40"),
            StaticGame("redfate-40-6", "Red Fate 40-6"),
            StaticGame("red-27", "Red 27"),
            StaticGame("sevens-play", "Sevens Play"),
            StaticGame("fruit-boom-5", "Fruit Boom 5"),
            StaticGame("fruit-boom-10", "Fruit Boom 10"),
            StaticGame("fruit-boom-20", "Fruit Boom 20"),
            StaticGame("pure-ecstasy", "Pure Ecstasy"),
            StaticGame("chica-alegre-5", "Chica Alegre 5"),
            StaticGame("chica-alegre-10", "Chica Alegre 10"),
            StaticGame("chica-alegre-20", "Chica Alegre 20"),
            StaticGame("regal-spins-40", "Regal Spins 40"),
            StaticGame("regal-spins-40-6", "Regal Spins 40-6"),
            StaticGame("regal-spins-100", "Regal Spins 100"),
            StaticGame("regal-spins-100-6", "Regal Spins 100-6"),
            StaticGame("luz-de-draco-5", "Luz de Draco 5"),
            StaticGame("luz-de-draco-10", "Luz de Draco 10"),
            StaticGame("luz-de-draco-20", "Luz de Draco 20"),
            StaticGame("luz-de-draco-40", "Luz de Draco 40"),
            StaticGame("luz-de-draco-40-6", "Luz de Draco 40-6"),
            StaticGame("luz-de-draco-100", "Luz de Draco 100"),
            StaticGame("luz-de-draco-100-6", "Luz de Draco 100-6"),
            StaticGame("pome-splash-5", "Pome Splash 5"),
            StaticGame("sevens-joy", "Sevens Joy"),
            StaticGame("noble-fate-5", "Noble Fate 5"),
            StaticGame("noble-fate-10", "Noble Fate 10"),
            StaticGame("noble-fate-20", "Noble Fate 20"),
            StaticGame("mr-first", "Mr First"),
            StaticGame("mr-first-10", "Mr First 10"),
            StaticGame("attractive-flirt-30", "Attractive Flirt 30"),
            StaticGame("attractive-flirt-50", "Attractive Flirt 50"),
            StaticGame("beasts-joy-30", "Beasts Joy 30"),
            StaticGame("beasts-joy-50", "Beasts Joy 50"),
            StaticGame("redfate-100", "Red Fate 100"),
            StaticGame("redfate-100-6", "Red Fate 100-6"),
            StaticGame("adamant-bang-20", "Adamant Bang 20"),
            StaticGame("adamant-bang-40", "Adamant Bang 40"),
            StaticGame("adamant-bang-50", "Adamant Bang 50"),
            StaticGame("splash-cascade-25", "Splash Cascade 25"),
            StaticGame("jester-bags-5-3", "Jester Bags 5-3"),
            StaticGame("mjolnir-splash-10", "Mjolnir Splash 10"),
            StaticGame("lost-in-giza-20", "Lost in Giza 20"),
            StaticGame("lost-in-giza-40", "Lost in Giza 40"),
            StaticGame("jester-bags-10-5", "Jester Bags 10-5"),
            StaticGame("sams-play", "Sams Play"),
            StaticGame("mr-money-bunny-10-5", "Mr Money Bunny 10-5"),
        )

        private data class StaticGame(
            val symbol: String,
            val name: String,
            val freeSpinEnable: Boolean = true,
            val demoEnable: Boolean = true
        )
    }
}
