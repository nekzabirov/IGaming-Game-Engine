package application.saga.spin.rollback.step

import application.port.outbound.PlayerLimitAdapter
import application.saga.ValidationStep
import application.saga.spin.rollback.RollbackSpinContext

/**
 * Restores the spin budget (spinMaxAmount) when a spin is rolled back.
 * Only applies to non-freespin rounds where a budget was set.
 */
class RestoreSpinBudgetStep(
    private val playerLimitAdapter: PlayerLimitAdapter
) : ValidationStep<RollbackSpinContext>("restore_spin_budget", "Restore Spin Budget") {

    override suspend fun execute(context: RollbackSpinContext): Result<Unit> {
        if (!context.isFreeSpin) {
            val originalAmount = context.originalSpin?.amount ?: 0L
            if (originalAmount > 0L) {
                playerLimitAdapter.increaseSpinMax(context.session.playerId, originalAmount)
            }
        }
        return Result.success(Unit)
    }
}
