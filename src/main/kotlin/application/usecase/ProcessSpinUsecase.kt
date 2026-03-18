package application.usecase

import application.event.SpinEvent
import application.port.external.IBackgroundTaskPort
import application.port.external.IEventPort
import application.port.external.IPlayerLimitPort
import application.port.storage.ISpinRepository
import application.port.external.IWalletPort
import domain.exception.forbidden.MaxPlaceSpinException
import domain.exception.domainRequire
import domain.model.PlayerBalance
import domain.model.Spin
import domain.model.SpinType
import domain.service.SpinBalanceCalculator
import domain.service.SpinResult
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class ProcessSpinUsecase(
    private val spinRepository: ISpinRepository,
    private val eventAdapter: IEventPort,
    private val walletPort: IWalletPort,
    private val playerLimitPort: IPlayerLimitPort,
    private val backgroundTaskPort: IBackgroundTaskPort,
) {

    suspend operator fun invoke(spin: Spin): Result<Response> = runCatching {
        val result = if (spin.round.freespinId != null) {
            processFreespin(spin)
        } else {
            process(spin)
        }

        val updatedSpin = spinRepository.save(result.spin)

        eventAdapter.publish(SpinEvent(spin = updatedSpin))

        Response(spin = updatedSpin, balance = result.balance)
    }

    private suspend fun processFreespin(spin: Spin): SpinResult {
        val balance = walletPort.findBalance(
            playerId = spin.round.session.playerId,
            currency = spin.round.session.currency
        )

        return SpinResult(spin = spin, balance = balance)
    }

    private suspend fun process(spin: Spin): SpinResult = coroutineScope {
        val resultAsync = async { calculateResult(spin) }

        checkLimits(spin)

        backgroundTaskPort.launch(action = {
            updateBalance(spin)
        })

        resultAsync.await()
    }

    private suspend fun updateBalance(spin: Spin) {
        if (spin.type == SpinType.PLACE)
            walletPort.withdraw(
                playerId = spin.round.session.playerId,
                transactionId = spin.round.session.id.toString(),
                currency = spin.round.session.currency,
                realAmount = spin.realAmount,
                bonusAmount = spin.bonusAmount
            )
        else
            walletPort.deposit(
                playerId = spin.round.session.playerId,
                transactionId = spin.round.session.id.toString(),
                currency = spin.round.session.currency,
                realAmount = spin.realAmount,
                bonusAmount = spin.bonusAmount
            )
    }

    private suspend fun calculateResult(spin: Spin): SpinResult {
        val session = spin.round.session

        val playerBalance = walletPort.findBalance(playerId = session.playerId, currency = session.currency)

        return SpinBalanceCalculator.process(balance = playerBalance, spin = spin)
    }

    private suspend fun checkLimits(spin: Spin) {
        if (spin.type != SpinType.PLACE) return

        val session = spin.round.session

        val playerMaxPlaceAmount = playerLimitPort.getMaxPlaceAmount(playerId = session.playerId) ?: return

        domainRequire(playerMaxPlaceAmount > spin.amount) { MaxPlaceSpinException() }
    }

    data class Response(val spin: Spin, val balance: PlayerBalance)
}