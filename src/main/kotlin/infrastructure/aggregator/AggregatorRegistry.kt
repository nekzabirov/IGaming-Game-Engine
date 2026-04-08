package infrastructure.aggregator

import application.port.external.IFreespinPort
import application.port.external.IGamePort
import application.port.factory.AggregatorAdapterProvider
import application.port.factory.IAggregatorFactory
import domain.exception.badrequest.AggregatorNotSupportedException
import domain.model.Aggregator

/**
 * Resolves aggregator adapters by looking up the [AggregatorAdapterProvider] whose
 * [AggregatorAdapterProvider.integration] matches [Aggregator.integration].
 *
 * Zero-touch extensibility: Koin's `getAll<AggregatorAdapterProvider>()` surfaces every
 * bound provider at boot. Adding a new aggregator never requires editing this class.
 */
class AggregatorRegistry(
    providers: List<AggregatorAdapterProvider>,
) : IAggregatorFactory {

    private val providersByIntegration: Map<String, AggregatorAdapterProvider> =
        providers.associateBy(AggregatorAdapterProvider::integration)

    override fun createGameAdapter(aggregator: Aggregator): IGamePort =
        resolve(aggregator).createGameAdapter(aggregator.config)

    override fun createFreespinAdapter(aggregator: Aggregator): IFreespinPort =
        resolve(aggregator).createFreespinAdapter(aggregator.config)

    private fun resolve(aggregator: Aggregator): AggregatorAdapterProvider =
        providersByIntegration[aggregator.integration]
            ?: throw AggregatorNotSupportedException(aggregator.integration)
}
