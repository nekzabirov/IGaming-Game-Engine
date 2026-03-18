package infrastructure.aggregator.pragmatic

import infrastructure.aggregator.pragmatic.adapter.PragmaticFreespinAdapter
import infrastructure.aggregator.pragmatic.adapter.PragmaticGameAdapter

class PragmaticAdapterFactory {

    fun createGameAdapter(config: Map<String, Any>) = PragmaticGameAdapter(createConfig(config))

    fun createFreespinAdapter(config: Map<String, Any>) = PragmaticFreespinAdapter(createConfig(config))

    private fun createConfig(config: Map<String, Any>) = PragmaticConfig(config)
}
