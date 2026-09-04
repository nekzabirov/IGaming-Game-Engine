package infrastructure.handler.game

import application.IQueryHandler
import application.query.game.GetCasinoGameDemoUrlQuery
import domain.exception.domainRequireNotNull
import domain.exception.notfound.CasinoGameNotFoundException
import domain.repository.ICasinoGameRepository
import infrastructure.gamehub.GameHubClient

class GetCasinoGameDemoUrlQueryHandler(
    private val gameRepository: ICasinoGameRepository,
    private val gameHubClient: GameHubClient,
) : IQueryHandler<GetCasinoGameDemoUrlQuery, String> {

    override suspend fun handle(query: GetCasinoGameDemoUrlQuery): String {
        val game = domainRequireNotNull(
            gameRepository.findByIdentity(query.identity)
        ) { CasinoGameNotFoundException() }

        return gameHubClient.launchCasinoDemo(
            game = game.identity.value,
            currency = query.currency.value,
            locale = query.locale.value,
            lobbyUrl = query.lobbyUrl,
            platform = query.platform,
        )
    }
}
