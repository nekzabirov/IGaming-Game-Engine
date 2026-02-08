package infrastructure.aggregator.onegamehub.adapter

import application.port.outbound.AggregatorGameSyncPort
import domain.aggregator.AggregatorGame
import domain.aggregator.AggregatorInfo
import domain.common.error.AggregatorError
import infrastructure.aggregator.onegamehub.model.OneGameHubConfig
import infrastructure.aggregator.onegamehub.client.OneGameHubHttpClient
import domain.common.value.Locale
import domain.common.value.Platform

/**
 * OneGameHub implementation for syncing games.
 */
class OneGameHubGameSyncAdapter(aggregatorInfo: AggregatorInfo) : AggregatorGameSyncPort {

    private val config = OneGameHubConfig(aggregatorInfo.config)
    private val client = OneGameHubHttpClient(config)

    override suspend fun listGames(): Result<List<AggregatorGame>> {
        val response = client.listGames().getOrElse {
            return Result.failure(it)
        }

        if (!response.success || response.response == null) return Result.failure(
            AggregatorError("Cannot load game from aggregator OneGameHub. status : ${response.status}")
        )

        return Result.success(response.response.map {
                    AggregatorGame(
                        symbol = it.id,
                        name = it.name,
                        providerName = it.brand,
                        freeSpinEnable = it.freespinEnable,
                        freeChipEnable = false,
                        jackpotEnable = false,
                        demoEnable = it.demoEnable,
                        bonusBuyEnable = true,
                        locales = SUPPORTED_LANGUAGES.map { lang -> Locale(lang) },
                        platforms = listOf(Platform.DESKTOP, Platform.MOBILE)
                    )
                })
    }

    companion object {
        private val SUPPORTED_LANGUAGES = listOf(
            "ab", "aa", "af", "ak", "sq", "am", "ar", "an", "hy", "as", "av", "ae", "ay", "az", "bm", "ba",
            "eu", "be", "bn", "bh", "bi", "bs", "br", "bg", "my", "ca", "ch", "ce", "ny", "zh", "cv", "kw",
            "co", "cr", "hr", "cs", "da", "dv", "nl", "dz", "en", "eo", "et", "ee", "fo", "fj", "fi", "fr",
            "ff", "gl", "ka", "de", "el", "gn", "gu", "ht", "ha", "he", "hz", "hi", "ho", "hu", "ia", "id",
            "ie", "ga", "ig", "ik", "io", "is", "it", "iu", "ja", "jv", "kl", "kn", "kr", "ks", "kk", "km",
            "ki", "rw", "ky", "kv", "kg", "ko", "ku", "kj", "la", "lb", "lg", "li", "ln", "lo", "lt", "lu",
            "lv", "gv", "mk", "mg", "ms", "ml", "mt", "mi", "mr", "mh", "mn", "na", "nv", "nd", "ne", "ng",
            "nb", "nn", "no", "ii", "nr", "oc", "oj", "cu", "om", "or", "os", "pa", "pi", "fa", "pl", "ps",
            "pt", "qu", "rm", "rn", "ro", "ru", "sa", "sc", "sd", "se", "sm", "sg", "sr", "gd", "sn", "si",
            "sk", "sl", "so", "st", "es", "su", "sw", "ss", "sv", "ta", "te", "tg", "th", "ti", "bo", "tk",
            "tl", "tn", "to", "tr", "ts", "tt", "tw", "ty", "ug", "uk", "ur", "uz", "ve", "vi", "vo", "wa",
            "cy", "wo", "fy", "xh", "yi", "yo", "za", "zu"
        )
    }
}
