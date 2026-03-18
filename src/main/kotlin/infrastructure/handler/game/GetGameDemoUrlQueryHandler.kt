package infrastructure.handler.game

import application.cqrs.IQueryHandler
import application.cqrs.game.GetGameDemoUrlQuery
import application.port.factory.IAggregatoryFactory
import application.port.storage.IGameVariantRepository
import domain.exception.domainRequireNotNull
import domain.exception.notfound.GameNotFoundException

class GetGameDemoUrlQueryHandler(
    private val gameVariantRepository: IGameVariantRepository,
    private val aggregatoryFactory: IAggregatoryFactory,
) : IQueryHandler<GetGameDemoUrlQuery, String> {

    override suspend fun handle(query: GetGameDemoUrlQuery): String {
        val gameVariant = domainRequireNotNull(
            gameVariantRepository.findActiveByGameIdentity(query.identity)
        ) { GameNotFoundException() }

        val gameAdapter = aggregatoryFactory.createGameAdapter(gameVariant.game.provider.aggregator)

        return gameAdapter.getDemoUrl(
            gameSymbol = gameVariant.symbol,
            locale = query.locale,
            platform = query.platform,
            currency = query.currency,
            lobbyUrl = query.lobbyUrl,
        )
    }
}
