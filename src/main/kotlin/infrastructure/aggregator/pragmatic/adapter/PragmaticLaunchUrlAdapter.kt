package infrastructure.aggregator.pragmatic.adapter

import application.port.outbound.AggregatorLaunchUrlPort
import domain.aggregator.AggregatorInfo
import infrastructure.aggregator.pragmatic.model.PragmaticConfig
import infrastructure.aggregator.pragmatic.client.PragmaticHttpClient
import infrastructure.aggregator.pragmatic.client.dto.LaunchUrlRequestDto
import shared.value.Currency
import domain.common.value.Locale
import domain.common.value.Platform

/**
 * Pragmatic implementation for getting game launch URLs.
 */
class PragmaticLaunchUrlAdapter(
    private val aggregatorInfo: AggregatorInfo
) : AggregatorLaunchUrlPort {

    private val config = PragmaticConfig(aggregatorInfo.config)
    private val client = PragmaticHttpClient(config)

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
        val payload = LaunchUrlRequestDto(
            gameSymbol = gameSymbol,
            sessionToken = sessionToken,
            playerId = playerId,
            locale = locale.value,
            platform = platform,
            currency = currency.value,
            lobbyUrl = lobbyUrl,
            demo = demo
        )

        return client.getLaunchUrl(payload)
    }
}
