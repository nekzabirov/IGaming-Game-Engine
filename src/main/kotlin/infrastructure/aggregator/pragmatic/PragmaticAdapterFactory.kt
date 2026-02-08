package infrastructure.aggregator.pragmatic

import application.port.outbound.AggregatorAdapterFactory
import application.port.outbound.AggregatorFreespinPort
import application.port.outbound.AggregatorGameSyncPort
import application.port.outbound.AggregatorLaunchUrlPort
import domain.aggregator.AggregatorInfo
import infrastructure.aggregator.pragmatic.adapter.PragmaticFreespinAdapter
import infrastructure.aggregator.pragmatic.adapter.PragmaticGameSyncAdapter
import infrastructure.aggregator.pragmatic.adapter.PragmaticLaunchUrlAdapter
import infrastructure.aggregator.shared.ProviderCurrencyAdapter
import domain.common.value.Aggregator

/**
 * Factory for creating Pragmatic aggregator adapters.
 */
class PragmaticAdapterFactory(private val providerCurrencyAdapter: ProviderCurrencyAdapter) : AggregatorAdapterFactory {

    override fun supports(aggregator: Aggregator): Boolean {
        return aggregator == Aggregator.PRAGMATIC
    }

    override fun createLaunchUrlAdapter(aggregatorInfo: AggregatorInfo): AggregatorLaunchUrlPort {
        return PragmaticLaunchUrlAdapter(aggregatorInfo)
    }

    override fun createFreespinAdapter(aggregatorInfo: AggregatorInfo): AggregatorFreespinPort {
        return PragmaticFreespinAdapter(aggregatorInfo, providerCurrencyAdapter)
    }

    override fun createGameSyncAdapter(aggregatorInfo: AggregatorInfo): AggregatorGameSyncPort {
        return PragmaticGameSyncAdapter(aggregatorInfo)
    }
}
