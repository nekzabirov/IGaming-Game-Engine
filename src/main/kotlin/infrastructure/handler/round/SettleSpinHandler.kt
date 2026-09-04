package infrastructure.handler.round

import application.ICommandHandler
import application.command.round.SettleSpinCommand
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

class SettleSpinHandler(
    private val gameRepository: ICasinoGameRepository,
    private val roundRepository: ICasinoRoundRepository,
    private val spinRepository: ISpinRepository,
    private val processSpinUsecase: ProcessSpinUsecase,
    private val walletPort: IWalletPort,
) : ICommandHandler<SettleSpinCommand, PlayerBalance> {

    override suspend fun handle(command: SettleSpinCommand): Result<PlayerBalance> = runCatching {
        val existingSpin = spinRepository.findByExternalId(command.externalSpinId)
        if (existingSpin != null) {
            return@runCatching walletPort.findBalance(command.playerId, command.currency)
        }

        val game = command.gameIdentity?.let { gameRepository.findByIdentity(it) }

        // A credit can legitimately be the first thing heard about a round — a win-only call for
        // a bonus payout — so the round opens here exactly as PlaceSpinHandler opens it.
        val round = roundRepository.findOrCreate(
            externalId = ExternalCasinoRoundId(command.externalRoundId),
            playerId = command.playerId,
            game = game,
            currency = command.currency,
            freespinId = command.freespinId?.let { FreespinId(it) },
        )

        val spin = SpinFactory.settle(
            round = round,
            externalId = ExternalSpinId(command.externalSpinId),
            amount = command.amount,
        )

        try {
            processSpinUsecase.invoke(spin).getOrThrow().balance
        } catch (_: SpinAlreadyExistsException) {
            walletPort.findBalance(command.playerId, command.currency)
        }
    }
}
