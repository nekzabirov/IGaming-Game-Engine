package application.saga.spin.place.step

import application.saga.ValidationStep
import application.saga.spin.place.PlaceSpinContext
import application.service.AggregatorService
import application.service.GameService
import domain.common.error.GameUnavailableError
import shared.Logger

/**
 * Step 1: Validate aggregator and game.
 */
class ValidateGameStep(
    private val aggregatorService: AggregatorService,
    private val gameService: GameService
) : ValidationStep<PlaceSpinContext>("validate_game", "Validate Game") {

    override suspend fun execute(context: PlaceSpinContext): Result<Unit> {
        val aggregator = aggregatorService.findById(context.session.aggregatorId).getOrElse {
            Logger.error("[ValidateGameStep] aggregator lookup failed for id=${context.session.aggregatorId}: ${it.message}")
            return Result.failure(it)
        }

        val game = gameService.findBySymbol(
            symbol = context.gameSymbol,
            aggregator = aggregator.aggregator
        ).getOrElse {
            Logger.error("[ValidateGameStep] game lookup failed for symbol=${context.gameSymbol}: ${it.message}")
            return Result.failure(it)
        }

        if (!game.isPlayable()) {
            Logger.warn("[ValidateGameStep] game unavailable symbol=${context.gameSymbol}")
            return Result.failure(GameUnavailableError(context.gameSymbol))
        }

        context.game = game
        return Result.success(Unit)
    }
}
