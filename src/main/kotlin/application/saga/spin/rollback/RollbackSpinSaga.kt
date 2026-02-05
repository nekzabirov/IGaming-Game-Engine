package application.saga.spin.rollback

import application.port.outbound.EventPublisherAdapter
import application.port.outbound.RoundRepository
import application.port.outbound.SpinRepository
import application.port.outbound.external.WalletAdapter
import application.saga.RetryPolicy
import application.saga.SagaOrchestrator
import application.saga.spin.rollback.step.*
import application.service.GameService

/**
 * Saga definition for rolling back a spin (refunding a bet).
 *
 * Step order:
 * 1. FindRoundWithSpin - find round AND place spin in single query (optimized)
 * 2. WalletRefund - refund the bet amount to wallet
 * 3. SaveRollbackSpin - save rollback spin record
 * 4. PublishEvent - publish rollback event
 */
class RollbackSpinSaga(
    private val gameService: GameService,
    private val walletAdapter: WalletAdapter,
    private val eventPublisher: EventPublisherAdapter,
    private val roundRepository: RoundRepository,
    private val spinRepository: SpinRepository
) {
    private val orchestrator = SagaOrchestrator(
        sagaName = "RollbackSpinSaga",
        steps = listOf(
            FindRoundWithSpinStep(roundRepository),
            WalletRefundStep(walletAdapter),
            SaveRollbackSpinStep(spinRepository),
            PublishRollbackEventStep(eventPublisher, gameService)
        ),
        retryPolicy = RetryPolicy.default()
    )

    suspend fun execute(context: RollbackSpinContext): Result<Unit> {
        return orchestrator.execute(context)
    }
}
