package application.saga

import domain.common.error.ExternalServiceError
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import shared.Logger as AppLogger

/**
 * Core saga orchestrator that manages step execution and compensation.
 * Implements the orchestration pattern for distributed transactions.
 *
 * Key features:
 * - Executes steps in order
 * - Retries failed steps based on retry policy
 * - Automatically compensates completed steps on failure
 * - Compensation runs in reverse order
 * - Non-cancellable compensation ensures rollback completes
 *
 * @param C The type of saga context
 * @param sagaName Human-readable name for logging
 * @param steps List of steps to execute in order
 * @param retryPolicy Retry policy for step execution
 * @param onStepComplete Optional callback when a step completes
 * @param onStepFailed Optional callback when a step fails
 */
class SagaOrchestrator<C : SagaContext>(
    private val sagaName: String,
    private val steps: List<SagaStep<C>>,
    private val retryPolicy: RetryPolicy = RetryPolicy.default(),
    private val onStepComplete: (suspend (stepId: String, status: StepStatus) -> Unit)? = null,
    private val onStepFailed: (suspend (stepId: String, error: Throwable) -> Unit)? = null,
    private val onSagaComplete: (suspend (sagaId: java.util.UUID, state: SagaState) -> Unit)? = null
) {
    private val logger = LoggerFactory.getLogger(SagaOrchestrator::class.java)

    /**
     * Execute the saga with all defined steps.
     * Automatically handles compensation on failure.
     *
     * @param context Saga context with input parameters
     * @return Result indicating success or the first error that occurred
     */
    suspend fun execute(context: C): Result<Unit> {
        logger.info("Starting saga [$sagaName] with id=${context.sagaId}")

        val completedSteps = mutableListOf<SagaStep<C>>()

        try {
            for (step in steps) {
                logger.info("Executing step [${step.stepName}] for saga ${context.sagaId}")
                onStepComplete?.invoke(step.stepId, StepStatus.EXECUTING)

                val result = executeWithRetry(step, context)

                result.onFailure { error ->
                    logger.error("Step [${step.stepName}] failed: ${error.message}")
                    onStepFailed?.invoke(step.stepId, error)
                    onStepComplete?.invoke(step.stepId, StepStatus.FAILED)

                    // Execute compensations in reverse order
                    compensate(context, completedSteps)

                    onSagaComplete?.invoke(context.sagaId, SagaState.COMPENSATED)
                    return Result.failure(error)
                }

                onStepComplete?.invoke(step.stepId, StepStatus.COMPLETED)
                completedSteps.add(step)
            }

            onSagaComplete?.invoke(context.sagaId, SagaState.COMPLETED)
            logger.info("Saga [$sagaName] completed successfully")
            return Result.success(Unit)

        } catch (e: Exception) {
            logger.error("Unexpected error in saga [$sagaName]: ${e.message}", e)
            compensate(context, completedSteps)
            onSagaComplete?.invoke(context.sagaId, SagaState.FAILED)
            return Result.failure(
                ExternalServiceError("Saga", "Unexpected failure: ${e.message}", e)
            )
        }
    }

    private suspend fun executeWithRetry(
        step: SagaStep<C>,
        context: C
    ): Result<Unit> {
        var lastError: Throwable? = null

        repeat(retryPolicy.maxAttempts) { attempt ->
            val result = AppLogger.profileSuspend("$sagaName.${step.stepName}") {
                try {
                    step.execute(context)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

            if (result.isSuccess) return result

            lastError = result.exceptionOrNull()

            if (attempt < retryPolicy.maxAttempts - 1 && retryPolicy.shouldRetry(lastError)) {
                logger.warn("Step [${step.stepName}] failed, retrying (${attempt + 1}/${retryPolicy.maxAttempts})")
                delay(retryPolicy.delayMs * (attempt + 1))
            }
        }

        return Result.failure(lastError ?: Exception("Unknown error"))
    }

    /**
     * Execute compensations for completed steps in reverse order.
     * Runs in NonCancellable context to ensure completion.
     */
    private suspend fun compensate(
        context: C,
        completedSteps: List<SagaStep<C>>
    ) = withContext(NonCancellable) {
        logger.info("Starting compensation for saga ${context.sagaId}")

        // Compensate in reverse order
        for (step in completedSteps.reversed()) {
            if (!step.requiresCompensation) continue

            logger.info("Compensating step [${step.stepName}]")

            try {
                AppLogger.profileSuspend("$sagaName.${step.stepName}.compensate") {
                    step.compensate(context)
                }.onFailure { error ->
                    logger.error("Compensation failed for step [${step.stepName}]: ${error.message}")
                    onStepComplete?.invoke(step.stepId, StepStatus.COMPENSATION_FAILED)
                    // Continue with other compensations even if one fails
                }
                onStepComplete?.invoke(step.stepId, StepStatus.COMPENSATED)
            } catch (e: Exception) {
                logger.error("Unexpected error during compensation of [${step.stepName}]", e)
                onStepComplete?.invoke(step.stepId, StepStatus.COMPENSATION_FAILED)
            }
        }
    }
}

