package infrastructure.aggregator.pateplay

import infrastructure.aggregator.pateplay.adapter.PateplayFreespinAdapter
import infrastructure.aggregator.pateplay.adapter.PateplayGameAdapter

class PateplayAdapterFactory {

    fun createGameAdapter(config: Map<String, Any>) = PateplayGameAdapter(createConfig(config))

    fun createFreespinAdapter(config: Map<String, Any>) = PateplayFreespinAdapter(createConfig(config))

    private fun createConfig(config: Map<String, Any>) = PateplayConfig(config)
}
