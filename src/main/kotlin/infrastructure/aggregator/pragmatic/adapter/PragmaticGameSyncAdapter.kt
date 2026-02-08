package infrastructure.aggregator.pragmatic.adapter

import application.port.outbound.AggregatorGameSyncPort
import domain.aggregator.AggregatorGame
import domain.aggregator.AggregatorInfo
import infrastructure.aggregator.pragmatic.client.PragmaticHttpClient
import infrastructure.aggregator.pragmatic.model.PragmaticConfig
import domain.common.value.Locale
import domain.common.value.Platform

/**
 * Pragmatic implementation for syncing games.
 */
class PragmaticGameSyncAdapter(aggregatorInfo: AggregatorInfo) : AggregatorGameSyncPort {

    private val client = PragmaticHttpClient(PragmaticConfig(aggregatorInfo.config))

    override suspend fun listGames(): Result<List<AggregatorGame>> {
        val games = client.listGames().getOrElse {
            return Result.failure(it)
        }

        return Result.success(
            games.map { game ->
                AggregatorGame(
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
        )
    }

    /**
     * Parse platform string from Pragmatic API (e.g., "DESKTOP,MOBILE") to list of Platform enum.
     */
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

        /**
         * Pragmatic supports 182 ISO 639-1 language codes.
         */
        private val SUPPORTED_LOCALES = listOf(
            "en", "ru", "de", "fr", "es", "pt", "it", "pl", "uk", "tr",
            "zh", "ja", "ko", "vi", "th", "id", "ms", "ar", "he", "hi",
            "cs", "sk", "hu", "ro", "bg", "hr", "sr", "sl", "et", "lv",
            "lt", "fi", "sv", "no", "da", "nl", "el", "ka", "az", "kk"
        ).map { Locale(it) }
    }
}
