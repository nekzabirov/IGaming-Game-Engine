package infrastructure.handler.freespin

import application.IQueryHandler
import application.query.freespin.GetFreespinPresetsQuery
import domain.exception.domainRequireNotNull
import domain.exception.notfound.CasinoGameNotFoundException
import domain.repository.ICasinoGameRepository
import infrastructure.gamehub.GameHubClient

class GetFreespinPresetsQueryHandler(
    private val gameRepository: ICasinoGameRepository,
    private val gameHubClient: GameHubClient,
) : IQueryHandler<GetFreespinPresetsQuery, Map<String, Any>> {

    override suspend fun handle(query: GetFreespinPresetsQuery): Map<String, Any> {
        val game = domainRequireNotNull(
            gameRepository.findByIdentity(query.gameIdentity)
        ) { CasinoGameNotFoundException() }

        val response = gameHubClient.freespinPresets(game.identity.value)

        return buildMap {
            putAll(response.presetsMap)
            if (response.hasMinAmount()) put("minAmount", response.minAmount)
            if (response.hasMaxAmount()) put("maxAmount", response.maxAmount)
            if (response.hasMinCount()) put("minCount", response.minCount)
            if (response.hasMaxCount()) put("maxCount", response.maxCount)
        }
    }
}
