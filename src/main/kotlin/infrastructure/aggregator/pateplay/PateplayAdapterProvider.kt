package infrastructure.aggregator.pateplay

import application.port.external.IFreespinPort
import application.port.external.IGamePort
import application.port.factory.AggregatorAdapterProvider
import infrastructure.aggregator.pateplay.adapter.PateplayFreespinAdapter
import infrastructure.aggregator.pateplay.adapter.PateplayGameAdapter

class PateplayAdapterProvider : AggregatorAdapterProvider {

    override val integration: String = INTEGRATION

    override fun createGameAdapter(config: Map<String, Any>): IGamePort =
        PateplayGameAdapter(PateplayConfig(config))

    override fun createFreespinAdapter(config: Map<String, Any>): IFreespinPort =
        PateplayFreespinAdapter(PateplayConfig(config))

    companion object {
        const val INTEGRATION: String = "PATEPLAY"
    }
}
