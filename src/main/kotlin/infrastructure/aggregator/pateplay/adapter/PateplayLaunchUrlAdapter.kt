package infrastructure.aggregator.pateplay.adapter

import application.port.outbound.AggregatorLaunchUrlPort
import domain.aggregator.AggregatorInfo
import domain.common.error.AggregatorError
import infrastructure.aggregator.pateplay.model.PateplayConfig
import io.ktor.http.*
import shared.value.Currency
import domain.common.value.Locale
import domain.common.value.Platform

/**
 * Pateplay implementation for getting game launch URLs.
 * Note: This adapter generates URLs directly without API calls.
 */
class PateplayLaunchUrlAdapter(
    private val aggregatorInfo: AggregatorInfo
) : AggregatorLaunchUrlPort {

    private val config = PateplayConfig(aggregatorInfo.config)

    override suspend fun getLaunchUrl(
        gameSymbol: String,
        sessionToken: String,
        playerId: String,
        locale: Locale,
        platform: Platform,
        currency: Currency,
        lobbyUrl: String,
        demo: Boolean
    ): Result<String> {
        val baseHost = if (demo) config.gameDemoLaunchUrl else config.gameLaunchUrl

        if (baseHost.isBlank()) {
            return Result.failure(AggregatorError("PatePlay game launch URL not configured"))
        }

        val url = URLBuilder("https://$baseHost").apply {
            parameters.append("siteCode", config.siteCode)
            parameters.append("authCode", sessionToken)
            parameters.append("playerId", playerId)
            parameters.append("language", locale.value)
            parameters.append("device", platform.toPateplayDevice())
            parameters.append("game", gameSymbol)
        }.buildString()

        return Result.success(url)
    }

    private fun Platform.toPateplayDevice(): String = when (this) {
        Platform.DESKTOP -> "desktop"
        Platform.MOBILE -> "mobile"
        Platform.DOWNLOAD -> "web"
    }
}
