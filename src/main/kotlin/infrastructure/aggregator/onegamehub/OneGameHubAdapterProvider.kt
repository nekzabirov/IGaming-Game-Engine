package infrastructure.aggregator.onegamehub

import application.port.external.IFreespinPort
import application.port.external.IGamePort
import application.port.factory.AggregatorAdapterProvider
import infrastructure.aggregator.onegamehub.adapter.OneGameHubFreespinAdapter
import infrastructure.aggregator.onegamehub.adapter.OneGameHubGameAdapter

class OneGameHubAdapterProvider : AggregatorAdapterProvider {

    override val integration: String = INTEGRATION

    override fun createGameAdapter(config: Map<String, Any>): IGamePort =
        OneGameHubGameAdapter(OneGameHubConfig(config))

    override fun createFreespinAdapter(config: Map<String, Any>): IFreespinPort =
        OneGameHubFreespinAdapter(OneGameHubConfig(config))

    companion object {
        const val INTEGRATION: String = "ONEGAMEHUB"
    }
}
