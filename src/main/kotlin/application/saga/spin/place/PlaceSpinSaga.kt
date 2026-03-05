package application.saga.spin.place

import application.port.outbound.EventPublisherAdapter
import application.port.outbound.PlayerLimitAdapter
import application.port.outbound.RoundRepository
import application.port.outbound.SpinRepository
import application.port.outbound.external.WalletAdapter
import application.saga.RetryPolicy
import application.saga.SagaOrchestrator
import application.saga.spin.place.step.*
import application.service.AggregatorService
import application.service.GameService

/**
 * Saga definition for placing a spin (bet) operation.
 * Ensures atomic execution with automatic compensation on failure.
 *
 * **CRITICAL FIX**: This saga executes wallet withdrawal BEFORE saving spin.
 * Previous implementation had a bug where spin was saved first, causing
 * orphan records if wallet withdrawal failed.
 *
 * **OPTIMIZATION**: Steps 2 & 3 now run in PARALLEL via PrepareRoundAndBalanceStep.
 *
 * Step order:
 * 1. ValidateGame - validate aggregator and game exist
 * 2. PrepareRoundAndBalance - create round AND validate balance IN PARALLEL
 * 3. WalletWithdraw - withdraw from wallet (BEFORE saving)
 * 4. SaveSpin - save spin record (AFTER wallet success)
 * 5. PublishEvent - publish domain event
 */
class PlaceSpinSaga(
    private val aggregatorService: AggregatorService,
    private val gameService: GameService,
    private val walletAdapter: WalletAdapter,
    private val playerLimitAdapter: PlayerLimitAdapter,
    private val eventPublisher: EventPublisherAdapter,
    private val roundRepository: RoundRepository,
    private val spinRepository: SpinRepository
) {
    private val orchestrator = SagaOrchestrator(
        sagaName = "PlaceSpinSaga",
        steps = listOf(
            ValidateGameStep(aggregatorService, gameService),
            PrepareRoundAndBalanceStep(roundRepository, walletAdapter, playerLimitAdapter),
            WalletWithdrawStep(walletAdapter),
            SavePlaceSpinStep(spinRepository),
            PublishSpinPlacedEventStep(eventPublisher)
        ),
        retryPolicy = RetryPolicy.default()
    )

    suspend fun execute(context: PlaceSpinContext): Result<Unit> {
        return orchestrator.execute(context)
    }
}
