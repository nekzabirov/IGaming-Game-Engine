package infrastructure.aggregator.onegamehub

import application.port.outbound.AggregatorAdapterFactory
import application.port.outbound.AggregatorFreespinPort
import application.port.outbound.AggregatorGameSyncPort
import application.port.outbound.AggregatorLaunchUrlPort
import infrastructure.aggregator.onegamehub.adapter.OneGameHubCurrencyAdapter
import domain.aggregator.AggregatorInfo
import infrastructure.aggregator.onegamehub.adapter.OneGameHubFreespinAdapter
import infrastructure.aggregator.onegamehub.adapter.OneGameHubGameSyncAdapter
import infrastructure.aggregator.onegamehub.adapter.OneGameHubLaunchUrlAdapter
import domain.common.value.Aggregator

/**
 * Factory for creating OneGameHub aggregator adapters.
 */
class OneGameHubAdapterFactory(private val providerCurrencyAdapter: OneGameHubCurrencyAdapter) : AggregatorAdapterFactory {

    override fun supports(aggregator: Aggregator): Boolean {
        return aggregator == Aggregator.ONEGAMEHUB
    }

    override fun createLaunchUrlAdapter(aggregatorInfo: AggregatorInfo): AggregatorLaunchUrlPort {
        return OneGameHubLaunchUrlAdapter(aggregatorInfo)
    }

    override fun createFreespinAdapter(aggregatorInfo: AggregatorInfo): AggregatorFreespinPort {
        return OneGameHubFreespinAdapter(aggregatorInfo, providerCurrencyAdapter)
    }

    override fun createGameSyncAdapter(aggregatorInfo: AggregatorInfo): AggregatorGameSyncPort {
        return OneGameHubGameSyncAdapter(aggregatorInfo)
    }
}
