package infrastructure.aggregator.gamehub

import application.port.external.ICasinoGamePort
import application.port.external.IFreespinPort
import application.port.external.IJackpotStreamPort
import application.port.factory.AggregatorAdapterProvider
import domain.exception.conflict.JackpotStreamNotSupportedException
import infrastructure.aggregator.gamehub.adapter.GameHubFreespinAdapter
import infrastructure.aggregator.gamehub.adapter.GameHubGameAdapter

class GameHubAdapterProvider : AggregatorAdapterProvider {

    override val integration: String = INTEGRATION

    override fun createGameAdapter(config: Map<String, Any>): ICasinoGamePort =
        GameHubGameAdapter(GameHubConfig(config))

    override fun createFreespinAdapter(config: Map<String, Any>): IFreespinPort =
        GameHubFreespinAdapter(GameHubConfig(config))

    /** The hub publishes no jackpot feed; sportbook falls through to the interface default, which
     *  raises `SportbookNotSupportedException`. */
    override fun createJackpotStreamAdapter(config: Map<String, Any>): IJackpotStreamPort =
        throw JackpotStreamNotSupportedException()

    companion object {
        const val INTEGRATION: String = "GAMEHUB"
    }
}
