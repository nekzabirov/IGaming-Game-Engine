package infrastructure.aggregator.pragmatic.adapter

import application.port.external.IGamePort
import domain.model.Platform
import domain.model.Session
import domain.vo.Currency
import domain.vo.Locale
import infrastructure.aggregator.pragmatic.PragmaticConfig
import infrastructure.aggregator.pragmatic.client.PragmaticHttpClient
import infrastructure.aggregator.pragmatic.client.dto.LaunchUrlRequestDto

class PragmaticGameAdapter(
    config: PragmaticConfig,
) : IGamePort {

    private val client = PragmaticHttpClient(config)

    override suspend fun getAggregatorGames(): List<IGamePort.AggregatorGame> {
        val games = client.listGames()

        return games.map { game ->
            IGamePort.AggregatorGame(
                symbol = game.gameId,
                name = game.gameName,
                providerName = PROVIDER_NAME,
                freeSpinEnable = game.freespinEnable,
                freeChipEnable = game.freeChipEnable,
                jackpotEnable = game.features?.contains("JACKPOT") ?: false,
                demoEnable = game.demoEnable,
                bonusBuyEnable = game.features?.contains("BUY") ?: false,
                locales = SUPPORTED_LOCALES,
                platforms = parsePlatforms(game.platform),
                playLines = game.lines
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
        return client.getLaunchUrl(
            LaunchUrlRequestDto(
                gameSymbol = gameSymbol,
                sessionToken = "",
                playerId = "",
                locale = locale.value,
                platform = platform,
                currency = currency.value,
                lobbyUrl = lobbyUrl,
                demo = true
            )
        )
    }

    override suspend fun getLaunchUrl(session: Session, lobbyUrl: String): String {
        return client.getLaunchUrl(
            LaunchUrlRequestDto(
                gameSymbol = session.gameVariant.symbol.value,
                sessionToken = session.token.value,
                playerId = session.playerId.value,
                locale = session.locale.value,
                platform = session.platform,
                currency = session.currency.value,
                lobbyUrl = lobbyUrl,
                demo = false
            )
        )
    }

    private fun parsePlatforms(platformString: String?): List<Platform> {
        if (platformString.isNullOrBlank()) {
            return listOf(Platform.DESKTOP, Platform.MOBILE)
        }

        return platformString.split(",").mapNotNull { platform ->
            when (platform.trim().uppercase()) {
                "DESKTOP" -> Platform.DESKTOP
                "MOBILE" -> Platform.MOBILE
                "DOWNLOAD" -> Platform.DOWNLOAD
                else -> null
            }
        }.ifEmpty {
            listOf(Platform.DESKTOP, Platform.MOBILE)
        }
    }

    companion object {
        private const val PROVIDER_NAME = "Pragmatic Play"

        private val SUPPORTED_LOCALES = listOf(
            "en", "ru", "de", "fr", "es", "pt", "it", "pl", "uk", "tr",
            "zh", "ja", "ko", "vi", "th", "id", "ms", "ar", "he", "hi",
            "cs", "sk", "hu", "ro", "bg", "hr", "sr", "sl", "et", "lv",
            "lt", "fi", "sv", "no", "da", "nl", "el", "ka", "az", "kk"
        ).map { Locale(it) }
    }
}
