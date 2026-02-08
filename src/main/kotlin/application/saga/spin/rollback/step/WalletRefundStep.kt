package application.saga.spin.rollback.step

import application.port.outbound.external.WalletAdapter
import application.saga.SagaStep
import application.saga.spin.rollback.RollbackSpinContext
import shared.Logger

/**
 * Step 3: Refund the bet amount to wallet.
 */
class WalletRefundStep(
    private val walletAdapter: WalletAdapter
) : SagaStep<RollbackSpinContext> {

    override val stepId = "wallet_refund"
    override val stepName = "Wallet Refund"
    override val requiresCompensation = true

    override suspend fun execute(context: RollbackSpinContext): Result<Unit> {
        // Skip if freespin (no money was wagered)
        if (context.isFreeSpin) {
            return Result.success(Unit)
        }

        // Skip if nothing to refund
        if (context.refundRealAmount == 0L && context.refundBonusAmount == 0L) {
            return Result.success(Unit)
        }

        // Use saga ID as transaction ID for idempotency
        val txId = context.sagaId.toString()

        // Deposit the refund (reverse of withdraw)
        val newBalance = walletAdapter.deposit(
            playerId = context.session.playerId,
            transactionId = txId,
            currency = context.session.currency,
            realAmount = context.refundRealAmount,
            bonusAmount = context.refundBonusAmount
        ).getOrElse {
            Logger.error("[WalletRefundStep] refund failed for player=${context.session.playerId} tx=$txId real=${context.refundRealAmount} bonus=${context.refundBonusAmount}: ${it.message}")
            return Result.failure(it)
        }

        context.resultBalance = newBalance
        context.put(RollbackSpinContext.KEY_WALLET_REFUND_COMPLETED, true)
        return Result.success(Unit)
    }

    override suspend fun compensate(context: RollbackSpinContext): Result<Unit> {
        if (context.isFreeSpin) return Result.success(Unit)

        val walletRefundCompleted = context.get<Boolean>(RollbackSpinContext.KEY_WALLET_REFUND_COMPLETED) ?: false
        if (!walletRefundCompleted) return Result.success(Unit)

        // Rollback the refund (unlikely to be needed, but for safety)
        return walletAdapter.rollback(
            context.session.playerId,
            context.sagaId.toString()
        )
    }
}
