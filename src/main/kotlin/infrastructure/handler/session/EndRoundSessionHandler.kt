package infrastructure.handler.session

import application.ICommandHandler
import application.command.session.EndRoundSessionCommand
import application.usecase.FinishRoundUsecase
import domain.exception.domainRequireNotNull
import domain.exception.notfound.RoundNotFoundException
import domain.exception.notfound.SessionNotFoundException
import domain.repository.IRoundRepository
import domain.repository.ISessionRepository
import domain.vo.ExternalRoundId

class EndRoundSessionHandler(
    private val sessionRepository: ISessionRepository,
    private val roundRepository: IRoundRepository,
    private val finishRoundUsecase: FinishRoundUsecase,
) : ICommandHandler<EndRoundSessionCommand, Unit> {

    override suspend fun handle(command: EndRoundSessionCommand): Result<Unit> = runCatching {
        val session = domainRequireNotNull(
            sessionRepository.findByToken(command.sessionToken)
        ) { SessionNotFoundException() }

        val round = domainRequireNotNull(
            roundRepository.findByExternalIdAndSessionId(
                externalId = ExternalRoundId(command.externalRoundId),
                sessionId = session.id,
            )
        ) { RoundNotFoundException() }

        finishRoundUsecase.invoke(round).getOrThrow()
    }
}
