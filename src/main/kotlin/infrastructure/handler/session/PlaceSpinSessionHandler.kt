package infrastructure.handler.session

import application.cqrs.ICommandHandler
import application.cqrs.session.PlaceSpinSessionCommand
import application.port.storage.IRoundRepository
import application.port.storage.ISessionRepository
import application.usecase.ProcessSpinUsecase
import domain.exception.notfound.SessionNotFoundException
import domain.exception.domainRequireNotNull
import domain.model.PlayerBalance
import domain.service.RoundFactory
import domain.service.SpinFactory

class PlaceSpinSessionHandler(
    private val sessionRepository: ISessionRepository,
    private val roundRepository: IRoundRepository,
    private val processSpinUsecase: ProcessSpinUsecase
) : ICommandHandler<PlaceSpinSessionCommand, PlayerBalance> {

    override suspend fun handle(command: PlaceSpinSessionCommand): Result<PlayerBalance> = runCatching {
        val session = domainRequireNotNull(
            sessionRepository.findByToken(command.sessionToken)
        ) { SessionNotFoundException() }

        val round = roundRepository.findByExternalIdAndSessionId(
            externalId = command.externalRoundId,
            sessionId = session.id
        ) ?: RoundFactory.open(session = session, externalId = command.externalRoundId, freespinId = command.freespinId)

        val spin = SpinFactory.place(round = round, externalId = command.externalSpinId, amount = command.amount)

        processSpinUsecase.invoke(spin).getOrThrow().balance
    }

}