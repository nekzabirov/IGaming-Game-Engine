package infrastructure.aggregator.onegamehub.handler

import application.port.outbound.external.WalletAdapter
import application.saga.spin.end.EndSpinContext
import application.saga.spin.end.EndSpinSaga
import application.saga.spin.place.PlaceSpinContext
import application.saga.spin.place.PlaceSpinSaga
import application.saga.spin.rollback.RollbackSpinContext
import application.saga.spin.rollback.RollbackSpinSaga
import application.saga.spin.settle.SettleSpinContext
import application.saga.spin.settle.SettleSpinSaga
import application.service.SessionService
import infrastructure.aggregator.onegamehub.adapter.OneGameHubCurrencyAdapter
import infrastructure.aggregator.onegamehub.handler.dto.OneGameHubBetDto
import domain.common.error.BetLimitExceededError
import domain.common.error.DomainError
import domain.common.error.GameUnavailableError
import domain.common.error.InsufficientBalanceError
import domain.common.error.InvalidPresetError
import domain.common.error.SessionInvalidError
import domain.session.model.Session
import infrastructure.aggregator.onegamehub.handler.dto.OneGameHubResponse
import shared.Logger
import shared.value.SessionToken

class OneGameHubHandler(
    private val sessionService: SessionService,
    private val walletAdapter: WalletAdapter,
    private val currencyAdapter: OneGameHubCurrencyAdapter,
    private val placeSpinSaga: PlaceSpinSaga,
    private val settleSpinSaga: SettleSpinSaga,
    private val endSpinSaga: EndSpinSaga,
    private val rollbackSpinSaga: RollbackSpinSaga
) {
    suspend fun balance(token: SessionToken): OneGameHubResponse {
        val session = sessionService.findByToken(token).getOrElse {
            Logger.error("[OneGameHub] balance failed: session lookup error: ${it.message}")
            return it.toErrorResponse
        }

        return returnSuccess(session)
    }

    suspend fun bet(token: SessionToken, payload: OneGameHubBetDto): OneGameHubResponse {
        val start = System.currentTimeMillis()

        val session = sessionService.findByToken(token).getOrElse {
            Logger.error("[OneGameHub] bet failed: session lookup error for round=${payload.roundId}: ${it.message}")
            return it.toErrorResponse
        }
        val sessionTime = System.currentTimeMillis() - start

        val context = PlaceSpinContext(
            session = session,
            gameSymbol = payload.gameSymbol,
            extRoundId = payload.roundId,
            transactionId = payload.transactionId,
            freeSpinId = payload.freeSpinId,
            amount = currencyAdapter.convertProviderToSystem(payload.amount, session.currency)
        )

        val sagaStart = System.currentTimeMillis()
        placeSpinSaga.execute(context).getOrElse {
            Logger.error("[OneGameHub] bet saga failed for round=${payload.roundId} player=${session.playerId}: ${it.message}")
            return it.toErrorResponse
        }
        val sagaTime = System.currentTimeMillis() - sagaStart

        val totalTime = System.currentTimeMillis() - start
        Logger.info("[PROFILE] OneGameHub.place(round=${payload.roundId}) session={}ms saga={}ms total={}ms",
            sessionTime, sagaTime, totalTime)

        // Use balance from wallet response (no extra HTTP call!)
        val balance = context.resultBalance ?: return returnSuccess(session)
        return OneGameHubResponse.Success(
            balance = currencyAdapter.convertSystemToProvider(balance.totalAmount, balance.currency).toInt(),
            currency = balance.currency.value
        )
    }

    suspend fun win(token: SessionToken, payload: OneGameHubBetDto): OneGameHubResponse {
        val start = System.currentTimeMillis()

        val session = sessionService.findByToken(token).getOrElse {
            Logger.error("[OneGameHub] win failed: session lookup error for round=${payload.roundId}: ${it.message}")
            return it.toErrorResponse
        }

        val context = SettleSpinContext(
            session = session,
            extRoundId = payload.roundId,
            transactionId = payload.transactionId,
            freeSpinId = payload.freeSpinId,
            winAmount = currencyAdapter.convertProviderToSystem(payload.amount, session.currency)
        )

        settleSpinSaga.execute(context).getOrElse {
            Logger.error("[OneGameHub] win settle saga failed for round=${payload.roundId} player=${session.playerId}: ${it.message}")
            return it.toErrorResponse
        }

        if (payload.finishRound) {
            val endContext = EndSpinContext(
                session = session,
                extRoundId = payload.roundId,
                freeSpinId = payload.freeSpinId
            )
            endSpinSaga.execute(endContext).getOrElse {
                Logger.error("[OneGameHub] win endRound saga failed for round=${payload.roundId} player=${session.playerId}: ${it.message}")
                return it.toErrorResponse
            }
        }

        val totalTime = System.currentTimeMillis() - start
        Logger.info("[PROFILE] OneGameHub.settle(round=${payload.roundId}, finishRound=${payload.finishRound}) total={}ms", totalTime)

        // Use cached balance from settle saga if available
        val balance = context.resultBalance
        if (balance != null) {
            return OneGameHubResponse.Success(
                balance = currencyAdapter.convertSystemToProvider(balance.totalAmount, balance.currency).toInt(),
                currency = balance.currency.value
            )
        }

        // Fallback: fetch balance (only when winAmount=0 and saga skipped)
        return returnSuccess(session)
    }

    suspend fun cancel(token: SessionToken, roundId: String, transactionId: String): OneGameHubResponse =
        Logger.profileSuspend("OneGameHub.cancel(round=$roundId)") {
            val session = sessionService.findByToken(token).getOrElse {
                Logger.error("[OneGameHub] cancel failed: session lookup error for round=$roundId: ${it.message}")
                return@profileSuspend it.toErrorResponse
            }

            val context = RollbackSpinContext(
                session = session,
                extRoundId = roundId,
                transactionId = transactionId
            )

            rollbackSpinSaga.execute(context).getOrElse {
                Logger.error("[OneGameHub] cancel saga failed for round=$roundId player=${session.playerId}: ${it.message}")
                return@profileSuspend it.toErrorResponse
            }

            returnSuccess(session)
        }

    private suspend fun returnSuccess(session: Session): OneGameHubResponse {
        val balance = walletAdapter.findBalance(session.playerId, session.currency).getOrElse {
            Logger.error("[OneGameHub] balance lookup failed for player=${session.playerId}: ${it.message}")
            return it.toErrorResponse
        }

        return OneGameHubResponse.Success(
            balance = currencyAdapter.convertSystemToProvider(balance.totalAmount, balance.currency).toInt(),
            currency = balance.currency.value
        )
    }

    private val Throwable.toErrorResponse: OneGameHubResponse.Error
        get() = when (this) {
            is DomainError -> toErrorResponse
            else -> OneGameHubResponse.Error.UNEXPECTED_ERROR
        }

    private val DomainError.toErrorResponse: OneGameHubResponse.Error
        get() = when (this) {
            is BetLimitExceededError -> OneGameHubResponse.Error.EXCEED_WAGER_LIMIT
            is GameUnavailableError -> OneGameHubResponse.Error.UNAUTHORIZED
            is InsufficientBalanceError -> OneGameHubResponse.Error.INSUFFICIENT_FUNDS
            is InvalidPresetError -> OneGameHubResponse.Error.BONUS_BET_MAX_RESTRICTION
            is SessionInvalidError -> OneGameHubResponse.Error.SESSION_TIMEOUT
            else -> OneGameHubResponse.Error.UNEXPECTED_ERROR
        }
}
