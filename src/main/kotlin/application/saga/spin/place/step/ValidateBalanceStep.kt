package application.saga.spin.place.step

import application.port.outbound.PlayerAdapter
import application.port.outbound.external.WalletAdapter
import application.saga.ValidationStep
import application.saga.spin.place.PlaceSpinContext
import domain.common.error.BetLimitExceededError
import domain.common.error.IllegalStateError
import domain.common.error.InsufficientBalanceError
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * Step 3: Validate balance and bet limits (skip for freespins).
 */
class ValidateBalanceStep(
    private val walletAdapter: WalletAdapter,
    private val playerAdapter: PlayerAdapter
) : ValidationStep<PlaceSpinContext>("validate_balance", "Validate Balance") {

    override suspend fun execute(context: PlaceSpinContext): Result<Unit> {
        if (context.isFreeSpin) {
            // FreeSpin mode: skip balance validation
            context.betRealAmount = 0L
            context.betBonusAmount = 0L
            return Result.success(Unit)
        }

        val game = context.game ?: return Result.failure(
            IllegalStateError("validate_balance", "game not set")
        )

        // Fetch balance and bet limit in parallel for faster processing
        val (balanceResult, betLimitResult) = coroutineScope {
            val balanceDeferred = async { walletAdapter.findBalance(context.session.playerId) }
            val betLimitDeferred = async { playerAdapter.findCurrentBetLimit(context.session.playerId) }
            balanceDeferred.await() to betLimitDeferred.await()
        }

        val balance = balanceResult.getOrElse { return Result.failure(it) }
        val betLimit = betLimitResult.getOrElse { return Result.failure(it) }

        // Adjust balance if bonus bet is disabled
        val adjustedBalance = if (!game.bonusBetEnable) {
            balance.copy(bonus = 0L)
        } else {
            balance
        }

        // Validate bet limit
        if (betLimit != null && betLimit < context.amount) {
            return Result.failure(
                BetLimitExceededError(context.session.playerId, context.amount, betLimit)
            )
        }

        // Validate sufficient balance
        if (context.amount > adjustedBalance.totalAmount) {
            return Result.failure(
                InsufficientBalanceError(context.session.playerId, context.amount, adjustedBalance.totalAmount)
            )
        }

        // Calculate real and bonus amounts
        context.betRealAmount = minOf(context.amount, adjustedBalance.real)
        context.betBonusAmount = context.amount - context.betRealAmount
        context.balance = adjustedBalance

        return Result.success(Unit)
    }
}
