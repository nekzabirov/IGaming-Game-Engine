package application.saga.spin.place.step

import application.port.outbound.external.WalletAdapter
import application.saga.SagaStep
import application.saga.spin.place.PlaceSpinContext
import domain.session.model.Balance
import com.nekgamebling.infrastructure.external.BalanceCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import shared.Logger

/**
 * Step 3: Withdraw from wallet (ASYNC - fire and forget).
 *
 * OPTIMIZATION: Returns immediately with PREDICTED balance.
 * Wallet call runs in background for ~200ms faster response.
 *
 * Trade-off: If wallet fails, spin record exists but funds weren't deducted.
 * Reconciliation job should handle such orphan records.
 */
class WalletWithdrawStep(
    private val walletAdapter: WalletAdapter
) : SagaStep<PlaceSpinContext> {

    override val stepId = "wallet_withdraw"
    override val stepName = "Wallet Withdraw"
    override val requiresCompensation = false  // No compensation - async call

    override suspend fun execute(context: PlaceSpinContext): Result<Unit> {
        if (context.isFreeSpin) {
            // FreeSpin mode: skip wallet operation
            return Result.success(Unit)
        }

        val balance = context.balance ?: return Result.success(Unit)

        // Calculate PREDICTED balance immediately (no waiting for wallet)
        val predictedBalance = Balance(
            real = balance.real - context.betRealAmount,
            bonus = balance.bonus - context.betBonusAmount,
            currency = balance.currency
        )
        context.resultBalance = predictedBalance

        // Cache predicted balance for subsequent requests (e.g., win with amount=0)
        BalanceCache.put(context.session.playerId, predictedBalance)

        // Fire wallet call in background (don't wait)
        val txId = context.sagaId.toString()
        val playerId = context.session.playerId
        val currency = context.session.currency
        val realAmount = context.betRealAmount
        val bonusAmount = context.betBonusAmount

        CoroutineScope(Dispatchers.IO).launch {
            walletAdapter.withdraw(
                playerId = playerId,
                transactionId = txId,
                currency = currency,
                realAmount = realAmount,
                bonusAmount = bonusAmount
            ).onFailure { err ->
                // Log error - reconciliation job will handle orphan spins
                Logger.error("[WalletWithdrawStep] ASYNC wallet withdraw failed for tx=$txId player=$playerId: ${err.message}")
            }
        }

        return Result.success(Unit)
    }

    override suspend fun compensate(context: PlaceSpinContext): Result<Unit> {
        // No compensation needed - wallet call is async and idempotent
        return Result.success(Unit)
    }
}
