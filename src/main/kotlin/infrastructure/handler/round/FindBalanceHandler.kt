package infrastructure.handler.round

import application.IQueryHandler
import application.port.external.IWalletPort
import application.query.round.FindBalanceQuery
import domain.model.PlayerBalance

class FindBalanceHandler(
    private val walletPort: IWalletPort,
) : IQueryHandler<FindBalanceQuery, PlayerBalance> {

    override suspend fun handle(query: FindBalanceQuery): PlayerBalance =
        walletPort.findBalance(query.playerId, query.currency)
}
