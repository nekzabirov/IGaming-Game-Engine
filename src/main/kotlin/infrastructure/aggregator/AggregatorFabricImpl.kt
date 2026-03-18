package infrastructure.aggregator

import application.port.external.IFreespinPort
import application.port.external.IGamePort
import application.port.factory.IAggregatoryFactory
import domain.model.Aggregator
import infrastructure.aggregator.onegamehub.OneGamehubAdapterFactory
import infrastructure.aggregator.pateplay.PateplayAdapterFactory
import infrastructure.aggregator.pragmatic.PragmaticAdapterFactory

class AggregatorFabricImpl(
    private val oneGamehubAdapterFactory: OneGamehubAdapterFactory,
    private val pragmaticAdapterFactory: PragmaticAdapterFactory,
    private val pateplayAdapterFactory: PateplayAdapterFactory,
) : IAggregatoryFactory {

    override fun createGameAdapter(aggregator: Aggregator): IGamePort {
        return when (aggregator.integration) {
            "ONEGAMEHUB" -> oneGamehubAdapterFactory.createGameAdapter(aggregator.config)
            "PRAGMATIC" -> pragmaticAdapterFactory.createGameAdapter(aggregator.config)
            "PATEPLAY" -> pateplayAdapterFactory.createGameAdapter(aggregator.config)
            else -> error("Unsupported aggregator integration: ${aggregator.integration}")
        }
    }

    override fun createFreespinAdapter(aggregator: Aggregator): IFreespinPort {
        return when (aggregator.integration) {
            "ONEGAMEHUB" -> oneGamehubAdapterFactory.createFreespinAdapter(aggregator.config)
            "PRAGMATIC" -> pragmaticAdapterFactory.createFreespinAdapter(aggregator.config)
            "PATEPLAY" -> pateplayAdapterFactory.createFreespinAdapter(aggregator.config)
            else -> error("Unsupported aggregator integration: ${aggregator.integration}")
        }
    }
}