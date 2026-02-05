package application.saga.spin.settle

import application.port.outbound.EventPublisherAdapter
import application.port.outbound.RoundRepository
import application.port.outbound.SpinRepository
import application.port.outbound.external.WalletAdapter
import application.saga.RetryPolicy
import application.saga.SagaOrchestrator
import application.saga.spin.settle.step.*
import application.service.GameService

/**
 * Saga definition for settling a spin (determining win/loss).
 *
 * Step order:
 * 1. FindRoundWithSpin - find round AND place spin in single query (optimized)
 * 2. CalculateWinAmounts - determine real/bonus split
 * 3. WalletDeposit - deposit winnings (BEFORE saving)
 * 4. SaveSettleSpin - save settle spin record (AFTER wallet success)
 * 5. PublishEvent - publish domain event
 */
class SettleSpinSaga(
    private val gameService: GameService,
    private val walletAdapter: WalletAdapter,
    private val eventPublisher: EventPublisherAdapter,
    private val roundRepository: RoundRepository,
    private val spinRepository: SpinRepository
) {
    private val orchestrator = SagaOrchestrator(
        sagaName = "SettleSpinSaga",
        steps = listOf(
            FindRoundWithSpinStep(roundRepository),
            CalculateWinAmountsStep(),
            WalletDepositStep(walletAdapter),
            SaveSettleSpinStep(spinRepository),
            PublishSpinSettledEventStep(eventPublisher, gameService)
        ),
        retryPolicy = RetryPolicy.default()
    )

    suspend fun execute(context: SettleSpinContext): Result<Unit> {
        // Skip if no win amount
        if (context.winAmount <= 0L) {
            return Result.success(Unit)
        }
        return orchestrator.execute(context)
    }
}
