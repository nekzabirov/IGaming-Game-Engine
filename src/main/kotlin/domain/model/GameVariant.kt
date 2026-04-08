package domain.model

import domain.vo.GameSymbol
import domain.vo.Locale

data class GameVariant(
    val id: Long = Long.MIN_VALUE,

    val symbol: GameSymbol,

    val name: String,

    val integration: String,

    val game: Game,

    val providerName: String,

    val freeSpinEnable: Boolean,

    val freeChipEnable: Boolean,

    val jackpotEnable: Boolean,

    val demoEnable: Boolean,

    val bonusBuyEnable: Boolean,

    val locales: List<Locale>,

    val platforms: List<Platform>,

    val playLines: Int = 0,
) {
    fun supportsLocale(locale: Locale): Boolean = locales.contains(locale)

    fun supportsPlatform(platform: Platform): Boolean = platforms.contains(platform)
}
