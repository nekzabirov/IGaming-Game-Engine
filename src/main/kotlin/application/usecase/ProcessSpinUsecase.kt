package application.usecase

import application.port.external.IEventPublisherPort
import application.port.external.IPlayerLimitPort
import application.port.external.IWalletPort
import domain.event.SpinEvent
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

/**
 * [freespinToPayout] decides WHO settles a free round's winnings. On (the default) the win is
 * credited to the player's real balance right here, the way it always was. Off, no spin of a free
 * round touches the wallet at all — the win reaches the outside only as a `SpinEvent`, and whoever
 * owns the promotion (crm, which already tracks `settleAmount` on the grant) pays it out when the
 * grant closes. That is the point of the switch: a bonus must be settled once, by one owner, and
 * an installation picks which one.
 */
class ProcessSpinUsecase(
    private val spinRepository: ISpinRepository,
    private val eventPublisher: IEventPublisherPort,
    private val walletPort: IWalletPort,
    private val playerLimitPort: IPlayerLimitPort,
    private val freespinToPayout: Boolean,
) {

    private val logger = LoggerFactory.getLogger(ProcessSpinUsecase::class.java)

    suspend operator fun invoke(spin: Spin): Result<Response> = runCatching {
        logger.info(
            "Processing spin: type={} round={} amount={} freespin={}",
            spin.type, spin.round.id, spin.amount, spin.round.freespinId,
        )

        // A free round's BET costs nothing, so it neither checks nor moves the balance. Its
        // WINNINGS are ordinary money: with [freespinToPayout] they go through the normal path
        // and land on the wallet here; without it the whole round stays off the wallet and the
        // promotion's owner settles it, so the player is never paid twice for the same spin.
        val result = if (spin.round.freespinId != null && (spin.isPlace || !freespinToPayout)) {
            offWallet(spin)
        } else {
            process(spin)
        }

        val updatedSpin = spinRepository.save(result.spin)

        // Spin persisted: the event reflects committed state, so publish it now. A failed
        // wallet move (see process()) does NOT roll back this spin or this event — the committed
        // spin is the source of truth and the move is reconciled out-of-band. Likewise a broker
        // failure must never 500 the webhook once the spin is committed.
        try {
            eventPublisher.publish(SpinEvent(updatedSpin))
        } catch (e: Exception) {
            logger.error(
                "EVENT PUBLISH FAILED (event lost): route={} spinId={} externalId={} type={}",
                SpinEvent.route, updatedSpin.id, updatedSpin.externalId.value, updatedSpin.type, e,
            )
        }

        logger.info("Spin processed: id={} type={}", updatedSpin.id, updatedSpin.type)

        Response(spin = updatedSpin, balance = result.balance)
    }.onFailure { e ->
        if (e !is DomainException) {
            logger.error("Failed to process spin: type={} round={}", spin.type, spin.round.id, e)
        }
    }

    /**
     * Records the spin and answers with the balance as it stands. The spin keeps its `amount` — the
     * bet or the win — while its real/bonus split stays zero, which is what says "no money moved
     * here" to everything downstream, the event included.
     */
    private suspend fun offWallet(spin: Spin): SpinResult {
        val balance = walletPort.findBalance(
            playerId = spin.round.playerId,
            currency = spin.round.currency,
        )
        return SpinResult(spin = spin, balance = balance)
    }

    private suspend fun process(spin: Spin): SpinResult = coroutineScope {
        val resultAsync = async { calculateResult(spin) }

        checkLimits(spin)

        val result = resultAsync.await()

        // Debit/credit the wallet with the balance-split spin (real/bonus computed by
        // SpinBalanceCalculator). The raw `spin` has realAmount/bonusAmount = 0, so
        // using it would move nothing and leave the wallet balance unchanged.
        //
        // Awaited, not dispatched: the move has to land before the spin row commits, or a
        // concurrent redelivery that loses the unique-constraint race reads a wallet that has
        // not caught up and answers the provider a balance that is simply wrong. A failure is
        // still swallowed — the committed spin remains the source of truth and a broken wallet
        // move is reconciled out of band, exactly as before.
        runCatching { updateBalance(result.spin) }
            .onFailure { logger.error("Wallet move failed for spin {}", spin.externalId.value, it) }

        result
    }

    private suspend fun updateBalance(spin: Spin) {
        val round = spin.round
        // PLACE takes money, SETTLE gives it back. A ROLLBACK moves it opposite to the spin it
        // reverses, so rolling back a win is a withdrawal, not a deposit.
        val takesMoney = spin.isPlace || (spin.isRollback && spin.reference?.isSettle == true)
        val reference = reference(spin)
        if (takesMoney) {
            walletPort.withdraw(
                playerId = round.playerId,
                transactionId = reference,
                currency = round.currency,
                realAmount = spin.realAmount,
                bonusAmount = spin.bonusAmount,
            )
        } else {
            walletPort.deposit(
                playerId = round.playerId,
                transactionId = reference,
                currency = round.currency,
                realAmount = spin.realAmount,
                bonusAmount = spin.bonusAmount,
            )
        }
    }

    /**
     * The wallet key of ONE movement. It has to name the spin, not the round, so the wallet is
     * idempotent by this string, and a round-wide key would make every spin after the first look
     * like a retry of it and move no money at all. `externalId` is the hub's own leg id and is
     * already unique-constrained here, and the type separates the place from the settle that
     * follows it — which is also what makes a redelivered call harmless.
     */
    private fun reference(spin: Spin): String =
        "spin:${spin.type.name.lowercase()}:${spin.externalId.value}"

    private suspend fun calculateResult(spin: Spin): SpinResult {
        val round = spin.round
        val playerBalance = walletPort.findBalance(playerId = round.playerId, currency = round.currency)
        return SpinBalanceCalculator.process(balance = playerBalance, spin = spin)
    }

    private suspend fun checkLimits(spin: Spin) {
        if (!spin.isPlace) return

        val playerMaxPlaceAmount = playerLimitPort.getMaxPlaceAmount(playerId = spin.round.playerId) ?: return

        domainRequire(playerMaxPlaceAmount > spin.amount) { MaxPlaceSpinException() }
    }

    data class Response(val spin: Spin, val balance: PlayerBalance)
}
