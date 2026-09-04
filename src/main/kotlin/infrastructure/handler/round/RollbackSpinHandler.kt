package infrastructure.handler.round

import application.ICommandHandler
import application.command.round.RollbackSpinCommand
import application.port.external.IWalletPort
import application.usecase.ProcessSpinUsecase
import domain.exception.domainRequireNotNull
import domain.exception.notfound.SpinNotFoundException
import domain.model.PlayerBalance
import domain.repository.ISpinRepository
import domain.service.SpinFactory
import domain.vo.ExternalSpinId

class RollbackSpinHandler(
    private val spinRepository: ISpinRepository,
    private val processSpinUsecase: ProcessSpinUsecase,
    private val walletPort: IWalletPort,
) : ICommandHandler<RollbackSpinCommand, PlayerBalance> {

    override suspend fun handle(command: RollbackSpinCommand): Result<PlayerBalance> = runCatching {
        val original = domainRequireNotNull(
            spinRepository.findByExternalId(command.externalSpinId)
        ) { SpinNotFoundException() }

        // The rollback id is derived from the leg it reverses, which makes it unique and makes
        // redelivery cheap to detect: the hub retries a rollback until it gets an answer.
        val rollbackId = "${command.externalSpinId}$ROLLBACK_SUFFIX"

        val existing = spinRepository.findByExternalId(rollbackId)
        if (existing == null) {
            val spin = SpinFactory.rollback(
                round = original.round,
                externalId = ExternalSpinId(rollbackId),
                reference = original,
            )
            processSpinUsecase.invoke(spin).getOrThrow()
        }

        walletPort.findBalance(original.round.playerId, original.round.currency)
    }

    private companion object {
        const val ROLLBACK_SUFFIX = ":rollback"
    }
}
