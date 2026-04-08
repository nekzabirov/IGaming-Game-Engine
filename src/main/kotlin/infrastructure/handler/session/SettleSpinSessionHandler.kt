package infrastructure.handler.session

import application.ICommandHandler
import application.command.session.SettleSpinSessionCommand
import application.usecase.ProcessSpinUsecase
import domain.exception.domainRequireNotNull
import domain.exception.notfound.RoundNotFoundException
import domain.exception.notfound.SessionNotFoundException
import domain.model.PlayerBalance
import domain.repository.IRoundRepository
import domain.repository.ISessionRepository
import domain.service.SpinFactory
import domain.vo.ExternalRoundId
import domain.vo.ExternalSpinId

class SettleSpinSessionHandler(
    private val sessionRepository: ISessionRepository,
    private val roundRepository: IRoundRepository,
    private val processSpinUsecase: ProcessSpinUsecase,
) : ICommandHandler<SettleSpinSessionCommand, PlayerBalance> {

    override suspend fun handle(command: SettleSpinSessionCommand): Result<PlayerBalance> = runCatching {
        val session = domainRequireNotNull(
            sessionRepository.findByToken(command.sessionToken)
        ) { SessionNotFoundException() }

        val round = domainRequireNotNull(
            roundRepository.findByExternalIdAndSessionId(
                externalId = ExternalRoundId(command.externalRoundId),
                sessionId = session.id,
            )
        ) { RoundNotFoundException() }

        val spin = SpinFactory.settle(
            round = round,
            externalId = ExternalSpinId(command.externalSpinId),
            amount = command.amount,
        )

        processSpinUsecase.invoke(spin).getOrThrow().balance
    }
}
