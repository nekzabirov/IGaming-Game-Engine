package application.saga.spin.rollback.step

import application.port.outbound.RoundRepository
import application.saga.ValidationStep
import application.saga.spin.rollback.RollbackSpinContext
import domain.common.error.RoundNotFoundError
import shared.Logger

/**
 * Optimized step: Find round AND place spin in a single query.
 * Replaces separate FindRoundStep + FindOriginalSpinStep for better performance.
 */
class FindRoundWithSpinStep(
    private val roundRepository: RoundRepository
) : ValidationStep<RollbackSpinContext>("find_round_with_spin", "Find Round With Spin") {

    override suspend fun execute(context: RollbackSpinContext): Result<Unit> {
        val (round, originalSpin) = roundRepository.findWithPlaceSpin(
            context.session.id,
            context.extRoundId
        ) ?: run {
            Logger.warn("[FindRoundWithSpinStep] rollback: round not found extRoundId=${context.extRoundId} session=${context.session.id}")
            return Result.failure(RoundNotFoundError(context.extRoundId))
        }

        context.round = round
        context.originalSpin = originalSpin
        context.refundRealAmount = originalSpin.realAmount
        context.refundBonusAmount = originalSpin.bonusAmount
        return Result.success(Unit)
    }
}
