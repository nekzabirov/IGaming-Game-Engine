package application.usecase

import application.port.external.IBackgroundTaskPort
import application.port.external.IEventPort
import application.port.external.IPlayerLimitPort
import application.port.external.IWalletPort
import domain.event.toDomainEvent
import domain.exception.DomainException
import domain.exception.domainRequire
import domain.exception.forbidden.MaxPlaceSpinException
import domain.model.PlayerBalance
import domain.model.Spin
import domain.repository.ISpinRepository
import domain.service.SpinBalanceCalculator
import domain.service.SpinResult
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory

class ProcessSpinUsecase(
    private val spinRepository: ISpinRepository,
    private val eventPort: IEventPort,
    private val walletPort: IWalletPort,
    private val playerLimitPort: IPlayerLimitPort,
    private val backgroundTaskPort: IBackgroundTaskPort,
) {

    private val logger = LoggerFactory.getLogger(ProcessSpinUsecase::class.java)

    suspend operator fun invoke(spin: Spin): Result<Response> = runCatching {
        logger.info(
            "Processing spin: type={} session={} round={} amount={} freespin={}",
            spin.type, spin.round.session.id, spin.round.id, spin.amount, spin.round.freespinId,
        )

        val result = if (spin.round.freespinId != null) {
            processFreespin(spin)
        } else {
            process(spin)
        }

        val updatedSpin = spinRepository.save(result.spin)

        eventPort.publish(updatedSpin.toDomainEvent())

        logger.info("Spin processed: id={} type={}", updatedSpin.id, updatedSpin.type)

        Response(spin = updatedSpin, balance = result.balance)
    }.onFailure { e ->
        if (e !is DomainException) {
            logger.error(
                "Failed to process spin: type={} session={} round={}",
                spin.type, spin.round.session.id, spin.round.id, e,
            )
        }
    }

    private suspend fun processFreespin(spin: Spin): SpinResult {
        val balance = walletPort.findBalance(
            playerId = spin.round.session.playerId,
            currency = spin.round.session.currency,
        )
        return SpinResult(spin = spin, balance = balance)
    }

    private suspend fun process(spin: Spin): SpinResult = coroutineScope {
        val resultAsync = async { calculateResult(spin) }

        checkLimits(spin)

        backgroundTaskPort.launch(action = { updateBalance(spin) })

        resultAsync.await()
    }

    private suspend fun updateBalance(spin: Spin) {
        val session = spin.round.session
        if (spin.isPlace) {
            walletPort.withdraw(
                playerId = session.playerId,
                transactionId = session.id.toString(),
                currency = session.currency,
                realAmount = spin.realAmount,
                bonusAmount = spin.bonusAmount,
            )
        } else {
            walletPort.deposit(
                playerId = session.playerId,
                transactionId = session.id.toString(),
                currency = session.currency,
                realAmount = spin.realAmount,
                bonusAmount = spin.bonusAmount,
            )
        }
    }

    private suspend fun calculateResult(spin: Spin): SpinResult {
        val session = spin.round.session
        val playerBalance = walletPort.findBalance(playerId = session.playerId, currency = session.currency)
        return SpinBalanceCalculator.process(balance = playerBalance, spin = spin)
    }

    private suspend fun checkLimits(spin: Spin) {
        if (!spin.isPlace) return

        val session = spin.round.session
        val playerMaxPlaceAmount = playerLimitPort.getMaxPlaceAmount(playerId = session.playerId) ?: return

        domainRequire(playerMaxPlaceAmount > spin.amount) { MaxPlaceSpinException() }
    }

    data class Response(val spin: Spin, val balance: PlayerBalance)
}
