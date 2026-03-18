package infrastructure.handler.session

import application.cqrs.ICommandHandler
import application.cqrs.session.SettleSpinSessionCommand
import application.port.storage.IRoundRepository
import application.port.storage.ISessionRepository
import application.usecase.ProcessSpinUsecase
import domain.exception.notfound.RoundNotFoundException
import domain.exception.notfound.SessionNotFoundException
import domain.exception.domainRequireNotNull
import domain.model.PlayerBalance
import domain.service.SpinFactory

class SettleSpinSessionHandler(
    private val sessionRepository: ISessionRepository,
    private val roundRepository: IRoundRepository,
    private val processSpinUsecase: ProcessSpinUsecase
) : ICommandHandler<SettleSpinSessionCommand, PlayerBalance> {

    override suspend fun handle(command: SettleSpinSessionCommand): Result<PlayerBalance> = runCatching {
        val session = domainRequireNotNull(
            sessionRepository.findByToken(command.sessionToken)
        ) { SessionNotFoundException() }

        val round = domainRequireNotNull(
            roundRepository.findByExternalIdAndSessionId(
                externalId = command.externalRoundId,
                sessionId = session.id
            )
        ) { RoundNotFoundException() }

        val spin = SpinFactory.settle(round = round, externalId = command.externalSpinId, amount = command.amount)

        processSpinUsecase.invoke(spin).getOrThrow().balance
    }

}
