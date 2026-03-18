package domain.model

import domain.vo.Locale

data class GameVariant(
    val id: Long = Long.MIN_VALUE,

    val symbol: String,

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

    val playLines: Int = 0
)
