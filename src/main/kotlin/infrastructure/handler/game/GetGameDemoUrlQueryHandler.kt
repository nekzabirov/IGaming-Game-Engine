package infrastructure.handler.game

import application.IQueryHandler
import application.query.game.GetGameDemoUrlQuery
import application.port.factory.IAggregatorFactory
import domain.repository.IGameVariantRepository
import domain.exception.domainRequireNotNull
import domain.exception.notfound.GameNotFoundException

class GetGameDemoUrlQueryHandler(
    private val gameVariantRepository: IGameVariantRepository,
    private val aggregatorFactory: IAggregatorFactory,
) : IQueryHandler<GetGameDemoUrlQuery, String> {

    override suspend fun handle(query: GetGameDemoUrlQuery): String {
        val gameVariant = domainRequireNotNull(
            gameVariantRepository.findActiveByGameIdentity(query.identity)
        ) { GameNotFoundException() }

        val gameAdapter = aggregatorFactory.createGameAdapter(gameVariant.game.provider.aggregator)

        return gameAdapter.getDemoUrl(
            gameSymbol = gameVariant.symbol.value,
            locale = query.locale,
            platform = query.platform,
            currency = query.currency,
            lobbyUrl = query.lobbyUrl,
        )
    }
}
