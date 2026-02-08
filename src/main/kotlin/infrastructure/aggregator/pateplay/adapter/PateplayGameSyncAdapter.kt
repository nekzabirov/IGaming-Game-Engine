package infrastructure.aggregator.pateplay.adapter

import application.port.outbound.AggregatorGameSyncPort
import domain.aggregator.AggregatorGame
import domain.aggregator.AggregatorInfo
import infrastructure.aggregator.pateplay.model.PateplayConfig
import domain.common.value.Locale
import domain.common.value.Platform

/**
 * Pateplay implementation for syncing games.
 * Note: PatePlay uses a static game catalog - no API endpoint for game discovery.
 */
class PateplayGameSyncAdapter(aggregatorInfo: AggregatorInfo) : AggregatorGameSyncPort {

    private val config = PateplayConfig(aggregatorInfo.config)

    override suspend fun listGames(): Result<List<AggregatorGame>> {
        // PatePlay provides a static game catalog
        // Games are defined by the adapter, not fetched from API
        return Result.success(
            STATIC_GAMES.map { game ->
                AggregatorGame(
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
        )
    }

    companion object {
        private const val PROVIDER_NAME = "PatePlay"

        private val SUPPORTED_LOCALES = listOf(
            "pp", "af", "am", "ar", "arn", "as", "az", "ba", "be", "bg", "bn", "bo", "br",
            "bs", "ca", "co", "cs", "cy", "da", "de", "dsb", "dv", "el", "en", "es", "et",
            "eu", "fa", "fi", "fil", "fo", "fr", "fy", "ga", "gd", "gl", "gsw", "gu", "ha",
            "he", "hi", "hr", "hsb", "hu", "hy", "id", "ig", "ii", "is", "it", "iu", "ja",
            "ka", "kk", "kl", "km", "kn", "ko", "kok", "ky", "lb", "lo", "lt", "lv", "mi",
            "mk", "ml", "mn", "moh", "mr", "ms", "mt", "my", "nb", "ne", "nl", "nn", "no",
            "nso", "oc", "or", "pa", "pl", "prs", "ps", "pt", "quc", "quz", "rm", "ro", "ru",
            "rw", "sa", "sah", "se", "si", "sk", "sl", "sma", "smj", "smn", "sms", "sq", "sr",
            "sv", "sw", "syr", "ta", "te", "tg", "th", "tk", "tn", "tr", "tt", "tzm", "ug",
            "uk", "ur", "uz", "vi", "wo", "xh", "yo", "zh", "zu"
        ).map { Locale(it) }

        // Static game catalog - update as needed based on PatePlay's offerings
        private val STATIC_GAMES = listOf(
            StaticGame("regal-spins-5", "Regal Spins 5", freeSpinEnable = true, demoEnable = true),
            StaticGame("regal-spins-10", "Regal Spins 10", freeSpinEnable = true, demoEnable = true),
            StaticGame("regal-spins-20", "Regal Spins 20", freeSpinEnable = true, demoEnable = true),
            StaticGame("fruit-chase-5", "Fruit Chase 5", freeSpinEnable = true, demoEnable = true),
            StaticGame("fruit-chase-10", "Fruit Chase 10", freeSpinEnable = true, demoEnable = true),
            StaticGame("fruit-chase-20", "Fruit Chase 20", freeSpinEnable = true, demoEnable = true),
            StaticGame("sevens-heat-5", "Sevens Heat 5", freeSpinEnable = true, demoEnable = true),
            StaticGame("sevens-heat-10", "Sevens Heat 10", freeSpinEnable = true, demoEnable = true),
            StaticGame("sevens-heat-20", "Sevens Heat 20", freeSpinEnable = true, demoEnable = true),
            StaticGame("sevens-heat-40", "Sevens Heat 40", freeSpinEnable = true, demoEnable = true),
            StaticGame("sevens-heat-100", "Sevens Heat 100", freeSpinEnable = true, demoEnable = true),
            StaticGame("lady-chance-5", "Lady Chance 5", freeSpinEnable = true, demoEnable = true),
            StaticGame("lady-chance-10", "Lady Chance 10", freeSpinEnable = true, demoEnable = true),
            StaticGame("lady-chance-20", "Lady Chance 20", freeSpinEnable = true, demoEnable = true),
            StaticGame("lady-chance-40", "Lady Chance 40", freeSpinEnable = true, demoEnable = true),
            StaticGame("scorching-reels-40", "Scorching Reels 40", freeSpinEnable = true, demoEnable = true),
            StaticGame("scorching-reels-100", "Scorching Reels 100", freeSpinEnable = true, demoEnable = true),
            StaticGame("redfate-5", "Red Fate 5", freeSpinEnable = true, demoEnable = true),
            StaticGame("redfate-10", "Red Fate 10", freeSpinEnable = true, demoEnable = true),
            StaticGame("redfate-20", "Red Fate 20", freeSpinEnable = true, demoEnable = true),
            StaticGame("redfate-40", "Red Fate 40", freeSpinEnable = true, demoEnable = true),
            StaticGame("redfate-40-6", "Red Fate 40-6", freeSpinEnable = true, demoEnable = true),
            StaticGame("red-27", "Red 27", freeSpinEnable = true, demoEnable = true),
            StaticGame("sevens-play", "Sevens Play", freeSpinEnable = true, demoEnable = true),
            StaticGame("fruit-boom-5", "Fruit Boom 5", freeSpinEnable = true, demoEnable = true),
            StaticGame("fruit-boom-10", "Fruit Boom 10", freeSpinEnable = true, demoEnable = true),
            StaticGame("fruit-boom-20", "Fruit Boom 20", freeSpinEnable = true, demoEnable = true),
            StaticGame("pure-ecstasy", "Pure Ecstasy", freeSpinEnable = true, demoEnable = true),
            StaticGame("chica-alegre-5", "Chica Alegre 5", freeSpinEnable = true, demoEnable = true),
            StaticGame("chica-alegre-10", "Chica Alegre 10", freeSpinEnable = true, demoEnable = true),
            StaticGame("chica-alegre-20", "Chica Alegre 20", freeSpinEnable = true, demoEnable = true),
            StaticGame("regal-spins-40", "Regal Spins 40", freeSpinEnable = true, demoEnable = true),
            StaticGame("regal-spins-40-6", "Regal Spins 40-6", freeSpinEnable = true, demoEnable = true),
            StaticGame("regal-spins-100", "Regal Spins 100", freeSpinEnable = true, demoEnable = true),
            StaticGame("regal-spins-100-6", "Regal Spins 100-6", freeSpinEnable = true, demoEnable = true),
            StaticGame("luz-de-draco-5", "Luz de Draco 5", freeSpinEnable = true, demoEnable = true),
            StaticGame("luz-de-draco-10", "Luz de Draco 10", freeSpinEnable = true, demoEnable = true),
            StaticGame("luz-de-draco-20", "Luz de Draco 20", freeSpinEnable = true, demoEnable = true),
            StaticGame("luz-de-draco-40", "Luz de Draco 40", freeSpinEnable = true, demoEnable = true),
            StaticGame("luz-de-draco-40-6", "Luz de Draco 40-6", freeSpinEnable = true, demoEnable = true),
            StaticGame("luz-de-draco-100", "Luz de Draco 100", freeSpinEnable = true, demoEnable = true),
            StaticGame("luz-de-draco-100-6", "Luz de Draco 100-6", freeSpinEnable = true, demoEnable = true),
            StaticGame("pome-splash-5", "Pome Splash 5", freeSpinEnable = true, demoEnable = true),
            StaticGame("sevens-joy", "Sevens Joy", freeSpinEnable = true, demoEnable = true),
            StaticGame("noble-fate-5", "Noble Fate 5", freeSpinEnable = true, demoEnable = true),
            StaticGame("noble-fate-10", "Noble Fate 10", freeSpinEnable = true, demoEnable = true),
            StaticGame("noble-fate-20", "Noble Fate 20", freeSpinEnable = true, demoEnable = true),
            StaticGame("mr-first", "Mr First", freeSpinEnable = true, demoEnable = true),
            StaticGame("mr-first-10", "Mr First 10", freeSpinEnable = true, demoEnable = true),
            StaticGame("attractive-flirt-30", "Attractive Flirt 30", freeSpinEnable = true, demoEnable = true),
            StaticGame("attractive-flirt-50", "Attractive Flirt 50", freeSpinEnable = true, demoEnable = true),
            StaticGame("beasts-joy-30", "Beasts Joy 30", freeSpinEnable = true, demoEnable = true),
            StaticGame("beasts-joy-50", "Beasts Joy 50", freeSpinEnable = true, demoEnable = true),
            StaticGame("redfate-100", "Red Fate 100", freeSpinEnable = true, demoEnable = true),
            StaticGame("redfate-100-6", "Red Fate 100-6", freeSpinEnable = true, demoEnable = true),
            StaticGame("adamant-bang-20", "Adamant Bang 20", freeSpinEnable = true, demoEnable = true),
            StaticGame("adamant-bang-40", "Adamant Bang 40", freeSpinEnable = true, demoEnable = true),
            StaticGame("adamant-bang-50", "Adamant Bang 50", freeSpinEnable = true, demoEnable = true),
            StaticGame("splash-cascade-25", "Splash Cascade 25", freeSpinEnable = true, demoEnable = true),
            StaticGame("jester-bags-5-3", "Jester Bags 5-3", freeSpinEnable = true, demoEnable = true),
            StaticGame("mjolnir-splash-10", "Mjolnir Splash 10", freeSpinEnable = true, demoEnable = true),
            StaticGame("lost-in-giza-20", "Lost in Giza 20", freeSpinEnable = true, demoEnable = true),
            StaticGame("lost-in-giza-40", "Lost in Giza 40", freeSpinEnable = true, demoEnable = true),
            StaticGame("jester-bags-10-5", "Jester Bags 10-5", freeSpinEnable = true, demoEnable = true),
            StaticGame("sams-play", "Sams Play", freeSpinEnable = true, demoEnable = true),
            StaticGame("mr-money-bunny-10-5", "Mr Money Bunny 10-5", freeSpinEnable = true, demoEnable = true)
        )

        private data class StaticGame(
            val symbol: String,
            val name: String,
            val freeSpinEnable: Boolean,
            val demoEnable: Boolean
        )
    }
}
