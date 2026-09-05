package infrastructure.handler.round

import application.ICommandHandler
import application.command.round.RollbackSpinCommand
import application.port.external.IWalletPort
import application.usecase.ProcessSpinUsecase
import domain.model.PlayerBalance
import domain.repository.ISpinRepository
import domain.service.SpinFactory
import domain.vo.ExternalSpinId

class RollbackSpinHandler(
    private val spinRepository: ISpinRepository,
    private val processSpinUsecase: ProcessSpinUsecase,
    private val walletPort: IWalletPort,
) : ICommandHandler<RollbackSpinCommand, PlayerBalance?> {

    override suspend fun handle(command: RollbackSpinCommand): Result<PlayerBalance?> = runCatching {
        // Nothing to reverse is a SUCCESS, not a miss. The vendor repeats a rollback until it gets
        // an answer, and the hub walks both legs of a transaction in one pass: a refusal here stops
        // that pass, so the bet behind an unknown win leg would never be given back. A leg can be
        // unknown for perfectly ordinary reasons — the vendor cancels a transaction we declined, or
        // sends the cancel before the leg itself ever reached us.
        //
        // The answer carries no balance, deliberately. RollbackSpinRequest names only the leg, so
        // with no leg row there is no player to read a balance for, and inventing one would be a
        // lie about somebody's money. proto3 leaves an unset message field absent, and the hub reads
        // only `id` off a rollback answer — it asks for the balance separately.
        val original = spinRepository.findByExternalId(command.externalSpinId)
            ?: return@runCatching null

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
