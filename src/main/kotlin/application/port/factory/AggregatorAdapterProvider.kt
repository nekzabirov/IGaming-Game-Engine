package application.port.factory

import application.port.external.IFreespinPort
import application.port.external.IGamePort

/**
 * Strategy for a single aggregator integration.
 *
 * Each aggregator (OneGameHub, Pragmatic, Pateplay, ...) implements this interface and
 * self-registers via Koin. The [AggregatorRegistry] dispatches requests by matching
 * [integration] against `Aggregator.integration`.
 *
 * Adding a new aggregator:
 * 1. Implement this interface in `infrastructure/aggregator/<name>/<Name>AdapterProvider.kt`
 * 2. Bind it in `ExternalModule`:
 *    `single { <Name>AdapterProvider() } bind AggregatorAdapterProvider::class`
 * 3. Done — the registry picks it up automatically via `getAll()`.
 */
interface AggregatorAdapterProvider {

    val integration: String

    fun createGameAdapter(config: Map<String, Any>): IGamePort

    fun createFreespinAdapter(config: Map<String, Any>): IFreespinPort
}
