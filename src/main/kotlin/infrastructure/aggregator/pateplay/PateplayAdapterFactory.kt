package infrastructure.aggregator.pateplay

import application.port.outbound.AggregatorAdapterFactory
import application.port.outbound.AggregatorFreespinPort
import application.port.outbound.AggregatorGameSyncPort
import application.port.outbound.AggregatorLaunchUrlPort
import domain.aggregator.AggregatorInfo
import infrastructure.aggregator.pateplay.adapter.PateplayFreespinAdapter
import infrastructure.aggregator.pateplay.adapter.PateplayGameSyncAdapter
import infrastructure.aggregator.pateplay.adapter.PateplayLaunchUrlAdapter
import infrastructure.aggregator.shared.ProviderCurrencyAdapter
import domain.common.value.Aggregator

/**
 * Factory for creating Pateplay aggregator adapters.
 */
class PateplayAdapterFactory(private val providerCurrencyAdapter: ProviderCurrencyAdapter) : AggregatorAdapterFactory {

    override fun supports(aggregator: Aggregator): Boolean {
        return aggregator == Aggregator.PATEPLAY
    }

    override fun createLaunchUrlAdapter(aggregatorInfo: AggregatorInfo): AggregatorLaunchUrlPort {
        return PateplayLaunchUrlAdapter(aggregatorInfo)
    }

    override fun createFreespinAdapter(aggregatorInfo: AggregatorInfo): AggregatorFreespinPort {
        return PateplayFreespinAdapter(aggregatorInfo, providerCurrencyAdapter)
    }

    override fun createGameSyncAdapter(aggregatorInfo: AggregatorInfo): AggregatorGameSyncPort {
        return PateplayGameSyncAdapter(aggregatorInfo)
    }
}
