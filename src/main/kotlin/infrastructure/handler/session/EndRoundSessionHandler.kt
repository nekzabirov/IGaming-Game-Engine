package infrastructure.handler.session

import application.cqrs.ICommandHandler
import application.cqrs.session.EndRoundSessionCommand
import application.port.storage.IRoundRepository
import application.port.storage.ISessionRepository
import application.usecase.FinishRoundUsecase
import domain.exception.domainRequireNotNull
import domain.exception.notfound.RoundNotFoundException
import domain.exception.notfound.SessionNotFoundException

class EndRoundSessionHandler(
    private val sessionRepository: ISessionRepository,
    private val roundRepository: IRoundRepository,
    private val finishRoundUsecase: FinishRoundUsecase
) : ICommandHandler<EndRoundSessionCommand, Unit> {

    override suspend fun handle(command: EndRoundSessionCommand): Result<Unit> = runCatching {
        val session = domainRequireNotNull(
            sessionRepository.findByToken(command.sessionToken)
        ) { SessionNotFoundException() }

        val round = domainRequireNotNull(
            roundRepository.findByExternalIdAndSessionId(
                externalId = command.externalRoundId,
                sessionId = session.id
            )
        ) { RoundNotFoundException() }

        finishRoundUsecase.invoke(round).getOrThrow()
    }
}
