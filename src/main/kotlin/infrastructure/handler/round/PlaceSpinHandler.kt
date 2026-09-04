package infrastructure.handler.round

import application.ICommandHandler
import application.command.round.PlaceSpinCommand
import application.port.external.IWalletPort
import application.usecase.ProcessSpinUsecase
import domain.exception.conflict.SpinAlreadyExistsException
import domain.model.PlayerBalance
import domain.repository.ICasinoGameRepository
import domain.repository.ICasinoRoundRepository
import domain.repository.ISpinRepository
import domain.service.SpinFactory
import domain.vo.ExternalCasinoRoundId
import domain.vo.ExternalSpinId
import domain.vo.FreespinId

class PlaceSpinHandler(
    private val gameRepository: ICasinoGameRepository,
    private val roundRepository: ICasinoRoundRepository,
    private val spinRepository: ISpinRepository,
    private val processSpinUsecase: ProcessSpinUsecase,
    private val walletPort: IWalletPort,
) : ICommandHandler<PlaceSpinCommand, PlayerBalance> {

    override suspend fun handle(command: PlaceSpinCommand): Result<PlayerBalance> = runCatching {
        // Replay guard: the hub redelivers on timeout, so this leg may already be committed.
        // Answer success with the current balance — no second spin row, no second wallet move.
        val existingSpin = spinRepository.findByExternalId(command.externalSpinId)
        if (existingSpin != null) {
            return@runCatching walletPort.findBalance(command.playerId, command.currency)
        }

        val game = command.gameIdentity?.let { gameRepository.findByIdentity(it) }

        val round = roundRepository.findOrCreate(
            externalId = ExternalCasinoRoundId(command.externalRoundId),
            playerId = command.playerId,
            game = game,
            currency = command.currency,
            freespinId = command.freespinId?.let { FreespinId(it) },
        )

        val spin = SpinFactory.place(
            round = round,
            externalId = ExternalSpinId(command.externalSpinId),
            amount = command.amount,
        )

        // The lookup above cannot see a redelivery still in flight, so the unique constraint on
        // the external id is what actually decides the winner. The loser answers exactly as the
        // lookup would have: success, with the balance the winner left behind.
        try {
            processSpinUsecase.invoke(spin).getOrThrow().balance
        } catch (_: SpinAlreadyExistsException) {
            walletPort.findBalance(command.playerId, command.currency)
        }
    }
}
