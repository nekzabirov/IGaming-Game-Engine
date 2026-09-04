package infrastructure.handler.round

import application.ICommandHandler
import application.command.round.CloseRoundCommand
import application.usecase.FinishCasinoRoundUsecase
import domain.exception.domainRequireNotNull
import domain.exception.notfound.CasinoRoundNotFoundException
import domain.repository.ICasinoRoundRepository
import domain.vo.ExternalCasinoRoundId

class CloseRoundHandler(
    private val roundRepository: ICasinoRoundRepository,
    private val finishRoundUsecase: FinishCasinoRoundUsecase,
) : ICommandHandler<CloseRoundCommand, Unit> {

    override suspend fun handle(command: CloseRoundCommand): Result<Unit> = runCatching {
        val round = domainRequireNotNull(
            roundRepository.findByExternalId(ExternalCasinoRoundId(command.externalRoundId))
        ) { CasinoRoundNotFoundException() }

        finishRoundUsecase.invoke(round).getOrThrow()
    }
}
