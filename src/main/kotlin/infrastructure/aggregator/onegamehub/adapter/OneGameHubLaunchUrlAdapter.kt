package infrastructure.aggregator.onegamehub.adapter

import application.port.outbound.AggregatorLaunchUrlPort
import domain.aggregator.AggregatorInfo
import domain.common.error.AggregatorError
import infrastructure.aggregator.onegamehub.model.OneGameHubConfig
import infrastructure.aggregator.onegamehub.client.OneGameHubHttpClient
import shared.value.Currency
import domain.common.value.Locale
import domain.common.value.Platform

/**
 * OneGameHub implementation for getting game launch URLs.
 */
class OneGameHubLaunchUrlAdapter(
    private val aggregatorInfo: AggregatorInfo
) : AggregatorLaunchUrlPort {

    private val config = OneGameHubConfig(aggregatorInfo.config)
    private val client = OneGameHubHttpClient(config)

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
        val response = client.getLaunchUrl(
            gameSymbol = gameSymbol,
            sessionToken = sessionToken,
            playerId = playerId,
            locale = locale.value,
            platform = platform,
            currency = currency.value,
            lobbyUrl = lobbyUrl,
            demo = demo
        ).getOrElse {
            return Result.failure(it)
        }

        if (!response.success) {
            return Result.failure(AggregatorError("Error to get launch url status: ${response.status}"))
        }

        return Result.success(response.response!!.gameUrl)
    }
}
