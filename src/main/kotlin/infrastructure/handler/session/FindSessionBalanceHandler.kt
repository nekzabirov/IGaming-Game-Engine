package infrastructure.handler.session

import application.cqrs.IQueryHandler
import application.cqrs.session.FindSessionBalanceQuery
import application.port.external.IWalletPort
import application.port.storage.ISessionRepository
import domain.model.PlayerBalance

class FindSessionBalanceHandler(
    private val walletAdapter: IWalletPort,
    private val sessionRepository: ISessionRepository,
) : IQueryHandler<FindSessionBalanceQuery, PlayerBalance> {

    override suspend fun handle(query: FindSessionBalanceQuery): PlayerBalance {
        val session = sessionRepository.findByToken(query.token) ?: error("Session not found")

        val playerId = session.playerId
        val currency = session.currency

        return walletAdapter.findBalance(playerId, currency)
    }

}