package application.saga.spin.settle.step

import application.port.outbound.external.WalletAdapter
import application.saga.SagaStep
import application.saga.spin.settle.SettleSpinContext
import shared.Logger

/**
 * Step 4: Deposit winnings to wallet (BEFORE saving settle spin).
 */
class WalletDepositStep(
    private val walletAdapter: WalletAdapter
) : SagaStep<SettleSpinContext> {

    override val stepId = "wallet_deposit"
    override val stepName = "Wallet Deposit"
    override val requiresCompensation = true

    override suspend fun execute(context: SettleSpinContext): Result<Unit> {
        if (context.isFreeSpin) {
            // FreeSpin mode: skip wallet operation
            return Result.success(Unit)
        }

        // Skip wallet call for zero-win (no deposit needed)
        if (context.realAmount == 0L && context.bonusAmount == 0L) {
            // resultBalance stays null - handler will fetch if needed
            return Result.success(Unit)
        }

        // Use saga ID as transaction ID for idempotency
        val txId = context.sagaId.toString()

        val newBalance = walletAdapter.deposit(
            playerId = context.session.playerId,
            transactionId = txId,
            currency = context.session.currency,
            realAmount = context.realAmount,
            bonusAmount = context.bonusAmount
        ).getOrElse {
            Logger.error("[WalletDepositStep] deposit failed for player=${context.session.playerId} tx=$txId real=${context.realAmount} bonus=${context.bonusAmount}: ${it.message}")
            return Result.failure(it)
        }

        context.resultBalance = newBalance
        context.put(SettleSpinContext.KEY_WALLET_TX_COMPLETED, true)
        return Result.success(Unit)
    }

    override suspend fun compensate(context: SettleSpinContext): Result<Unit> {
        if (context.isFreeSpin) return Result.success(Unit)

        val walletTxCompleted = context.get<Boolean>(SettleSpinContext.KEY_WALLET_TX_COMPLETED) ?: false
        if (!walletTxCompleted) return Result.success(Unit)

        // Rollback the wallet deposit (withdraw the winnings back)
        return walletAdapter.rollback(
            context.session.playerId,
            context.sagaId.toString()
        )
    }
}
