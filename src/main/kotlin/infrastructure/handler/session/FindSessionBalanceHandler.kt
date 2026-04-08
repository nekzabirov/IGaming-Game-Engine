package infrastructure.handler.session

import application.IQueryHandler
import application.query.session.FindSessionBalanceQuery
import application.port.external.IWalletPort
import domain.repository.ISessionRepository
import domain.exception.domainRequireNotNull
import domain.exception.notfound.SessionNotFoundException
import domain.model.PlayerBalance

class FindSessionBalanceHandler(
    private val walletAdapter: IWalletPort,
    private val sessionRepository: ISessionRepository,
) : IQueryHandler<FindSessionBalanceQuery, PlayerBalance> {

    override suspend fun handle(query: FindSessionBalanceQuery): PlayerBalance {
        val session = domainRequireNotNull(
            sessionRepository.findByToken(query.token)
        ) { SessionNotFoundException() }

        return walletAdapter.findBalance(session.playerId, session.currency)
    }
}