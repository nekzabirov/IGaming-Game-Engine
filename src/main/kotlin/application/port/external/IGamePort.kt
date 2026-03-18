package application.port.external

import domain.model.Platform
import domain.model.Session
import domain.vo.Currency
import domain.vo.Locale

interface IGamePort {
    data class AggregatorGame(
        val symbol: String,
        val name: String,
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

    suspend fun getAggregatorGames(): List<AggregatorGame>

    suspend fun getDemoUrl(
        gameSymbol: String,
        locale: Locale,
        platform: Platform,
        currency: Currency,
        lobbyUrl: String,
    ): String

    suspend fun getLunchUrl(session: Session, lobbyUrl: String): String
}