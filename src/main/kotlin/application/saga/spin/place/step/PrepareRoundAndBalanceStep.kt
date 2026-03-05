package application.saga.spin.place.step

import application.port.outbound.PlayerLimitAdapter
import application.port.outbound.RoundRepository
import application.port.outbound.external.WalletAdapter
import application.saga.SagaStep
import application.saga.spin.place.PlaceSpinContext
import domain.common.error.IllegalStateError
import domain.common.error.InsufficientBalanceError
import domain.common.error.SpinLimitExceededError
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import shared.Logger

/**
 * Combined step: Find/create round AND validate balance in PARALLEL.
 * Saves ~36ms by running DB and HTTP operations concurrently.
 */
class PrepareRoundAndBalanceStep(
    private val roundRepository: RoundRepository,
    private val walletAdapter: WalletAdapter,
    private val playerLimitAdapter: PlayerLimitAdapter
) : SagaStep<PlaceSpinContext> {

    override val stepId = "prepare_round_and_balance"
    override val stepName = "Prepare Round & Balance"
    override val requiresCompensation = true

    override suspend fun execute(context: PlaceSpinContext): Result<Unit> {
        val game = context.game ?: return Result.failure(
            IllegalStateError(stepId, "game not set in context")
        )

        // Run round creation and balance validation in PARALLEL
        val (round, balanceValidation) = coroutineScope {
            val roundDeferred = async {
                roundRepository.findOrCreate(
                    sessionId = context.session.id,
                    gameId = game.id,
                    extId = context.extRoundId
                )
            }

            val balanceDeferred = async {
                if (context.isFreeSpin) {
                    // FreeSpin mode: skip balance validation
                    BalanceValidation(
                        betRealAmount = 0L,
                        betBonusAmount = 0L,
                        balance = null
                    )
                } else {
                    validateBalance(context, game)
                }
            }

            roundDeferred.await() to balanceDeferred.await()
        }

        // Check for validation errors
        balanceValidation.error?.let { return Result.failure(it) }

        // Store results in context
        context.round = round
        context.balance = balanceValidation.balance
        context.betRealAmount = balanceValidation.betRealAmount
        context.betBonusAmount = balanceValidation.betBonusAmount

        // Decrease remaining spin budget
        if (!context.isFreeSpin) {
            playerLimitAdapter.decreaseSpinMax(context.session.playerId, context.amount)
            context.spinMaxAmount = balanceValidation.spinMaxAmountBefore
        }

        return Result.success(Unit)
    }

    private suspend fun validateBalance(
        context: PlaceSpinContext,
        game: domain.game.model.Game
    ): BalanceValidation {
        val balance = walletAdapter.findBalance(context.session.playerId, context.session.currency).getOrElse {
            Logger.error("[PrepareRoundAndBalanceStep] balance fetch failed for player=${context.session.playerId}: ${it.message}")
            return BalanceValidation(error = it)
        }

        val spinMaxAmount = playerLimitAdapter.getSpinMaxAmount(context.session.playerId)

        // Adjust balance if bonus bet is disabled
        val adjustedBalance = if (!game.bonusBetEnable) {
            balance.copy(bonus = 0L)
        } else {
            balance
        }

        // Validate spin limit
        if (spinMaxAmount != null && spinMaxAmount < context.amount) {
            Logger.warn("[PrepareRoundAndBalanceStep] spin limit exceeded for player=${context.session.playerId} amount=${context.amount} limit=$spinMaxAmount")
            return BalanceValidation(
                error = SpinLimitExceededError(context.session.playerId, context.amount, spinMaxAmount)
            )
        }

        // Validate sufficient balance
        if (context.amount > adjustedBalance.totalAmount) {
            Logger.warn("[PrepareRoundAndBalanceStep] insufficient balance for player=${context.session.playerId} required=${context.amount} available=${adjustedBalance.totalAmount}")
            return BalanceValidation(
                error = InsufficientBalanceError(context.session.playerId, context.amount, adjustedBalance.totalAmount)
            )
        }

        // Calculate real and bonus amounts
        val betRealAmount = minOf(context.amount, adjustedBalance.real)
        val betBonusAmount = context.amount - betRealAmount

        return BalanceValidation(
            betRealAmount = betRealAmount,
            betBonusAmount = betBonusAmount,
            balance = adjustedBalance,
            spinMaxAmountBefore = spinMaxAmount
        )
    }

    override suspend fun compensate(context: PlaceSpinContext): Result<Unit> {
        // Restore spin budget if it was decreased
        if (!context.isFreeSpin && context.spinMaxAmount != null) {
            playerLimitAdapter.increaseSpinMax(context.session.playerId, context.amount)
        }
        return Result.success(Unit)
    }

    private data class BalanceValidation(
        val betRealAmount: Long = 0L,
        val betBonusAmount: Long = 0L,
        val balance: domain.session.model.Balance? = null,
        val spinMaxAmountBefore: Long? = null,
        val error: Throwable? = null
    )
}
