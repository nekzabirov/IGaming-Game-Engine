package infrastructure.aggregator.onegamehub

import infrastructure.aggregator.onegamehub.adapter.OneGameHubFreespinAdapter
import infrastructure.aggregator.onegamehub.adapter.OneGameHubGameAdapter

class OneGamehubAdapterFactory {

    fun createGameAdapter(config: Map<String, Any>) = OneGameHubGameAdapter(createConfig(config))

    fun createFreespinAdapter(config: Map<String, Any>) = OneGameHubFreespinAdapter(createConfig(config))

    private fun createConfig(config: Map<String, Any>) = OneGameHubConfig(config)

}