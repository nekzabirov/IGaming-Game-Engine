package infrastructure.aggregator.pragmatic.handler

import application.port.outbound.external.WalletAdapter
import application.saga.spin.end.EndSpinContext
import application.saga.spin.end.EndSpinSaga
import application.saga.spin.place.PlaceSpinContext
import application.saga.spin.place.PlaceSpinSaga
import application.saga.spin.rollback.RollbackSpinContext
import application.saga.spin.rollback.RollbackSpinSaga
import application.saga.spin.settle.SettleSpinContext
import application.saga.spin.settle.SettleSpinSaga
import application.service.GameService
import application.service.SessionService
import infrastructure.aggregator.pragmatic.handler.dto.PragmaticBetPayload
import infrastructure.aggregator.pragmatic.handler.dto.PragmaticResponse
import infrastructure.aggregator.shared.ProviderCurrencyAdapter
import shared.Logger
import shared.value.SessionToken

class PragmaticHandler(
    private val sessionService: SessionService,
    private val walletAdapter: WalletAdapter,
    private val currencyAdapter: ProviderCurrencyAdapter,
    private val placeSpinSaga: PlaceSpinSaga,
    private val settleSpinSaga: SettleSpinSaga,
    private val endSpinSaga: EndSpinSaga,
    private val rollbackSpinSaga: RollbackSpinSaga,
    private val gameService: GameService
) {

    suspend fun authenticate(sessionToken: SessionToken): PragmaticResponse {
        val session = sessionService.findByToken(sessionToken).getOrElse {
            Logger.error("[Pragmatic] authenticate failed: session lookup error: ${it.message}")
            return it.toErrorResponse()
        }

        val balance = walletAdapter.findBalance(session.playerId, session.currency).getOrElse {
            Logger.error("[Pragmatic] authenticate failed: balance lookup error for player=${session.playerId}: ${it.message}")
            return it.toErrorResponse()
        }

        val cash = currencyAdapter.convertSystemToProvider(balance.real, balance.currency)
        val bonus = currencyAdapter.convertSystemToProvider(balance.bonus, balance.currency)

        return PragmaticResponse.Success(
            cash = cash.toString(),
            bonus = bonus.toString(),
            currency = balance.currency.value,
            userId = session.playerId
        )
    }

    suspend fun balance(sessionToken: SessionToken): PragmaticResponse = authenticate(sessionToken)

    suspend fun bet(sessionToken: SessionToken, payload: PragmaticBetPayload): PragmaticResponse =
        Logger.profileSuspend("Pragmatic.place(round=${payload.roundId})") {
            val session = sessionService.findByToken(sessionToken).getOrElse {
                Logger.error("[Pragmatic] bet failed: session lookup error for round=${payload.roundId}: ${it.message}")
                return@profileSuspend it.toErrorResponse()
            }

            val betAmount = currencyAdapter.convertProviderToSystem(payload.amount.toBigDecimal(), session.currency)

            val context = PlaceSpinContext(
                session = session,
                gameSymbol = payload.gameId,
                extRoundId = payload.roundId,
                transactionId = payload.reference,
                freeSpinId = payload.bonusCode,
                amount = betAmount
            )

            placeSpinSaga.execute(context).getOrElse {
                Logger.error("[Pragmatic] bet saga failed for round=${payload.roundId} player=${session.playerId}: ${it.message}")
                return@profileSuspend it.toErrorResponse()
            }

            // Use cached balance from saga (no extra HTTP call!)
            val beforeBalance = context.balance
            val currentBalance = context.resultBalance

            if (currentBalance != null && beforeBalance != null) {
                val usedBonus = beforeBalance.bonus - currentBalance.bonus
                PragmaticResponse.Success(
                    cash = currencyAdapter.convertSystemToProvider(currentBalance.real, currentBalance.currency).toString(),
                    bonus = currencyAdapter.convertSystemToProvider(currentBalance.bonus, currentBalance.currency).toString(),
                    currency = currentBalance.currency.value,
                    usedPromo = currencyAdapter.convertSystemToProvider(usedBonus, currentBalance.currency).toString(),
                    transactionId = payload.reference
                )
            } else {
                // Fallback for freespin (no wallet operation)
                val balance = walletAdapter.findBalance(session.playerId, session.currency).getOrElse {
                    return@profileSuspend it.toErrorResponse()
                }
                PragmaticResponse.Success(
                    cash = currencyAdapter.convertSystemToProvider(balance.real, balance.currency).toString(),
                    bonus = currencyAdapter.convertSystemToProvider(balance.bonus, balance.currency).toString(),
                    currency = balance.currency.value,
                    usedPromo = "0",
                    transactionId = payload.reference
                )
            }
        }

    suspend fun result(sessionToken: SessionToken, payload: PragmaticBetPayload): PragmaticResponse =
        Logger.profileSuspend("Pragmatic.settle(round=${payload.roundId})") {
            val session = sessionService.findByToken(sessionToken).getOrElse {
                Logger.error("[Pragmatic] result failed: session lookup error for round=${payload.roundId}: ${it.message}")
                return@profileSuspend it.toErrorResponse()
            }

            val totalAmount = payload.amount.toBigDecimal() + payload.promoWinAmount.toBigDecimal()

            val context = SettleSpinContext(
                session = session,
                extRoundId = payload.roundId,
                transactionId = payload.reference,
                freeSpinId = payload.bonusCode,
                winAmount = currencyAdapter.convertProviderToSystem(totalAmount, session.currency)
            )

            settleSpinSaga.execute(context).getOrElse {
                Logger.error("[Pragmatic] settle saga failed for round=${payload.roundId} player=${session.playerId}: ${it.message}")
                return@profileSuspend it.toErrorResponse()
            }

            // Use cached balance from saga (no extra HTTP call!)
            val currentBalance = context.resultBalance
            if (currentBalance != null) {
                PragmaticResponse.Success(
                    cash = currencyAdapter.convertSystemToProvider(currentBalance.real, currentBalance.currency).toString(),
                    bonus = currencyAdapter.convertSystemToProvider(currentBalance.bonus, currentBalance.currency).toString(),
                    currency = currentBalance.currency.value,
                    transactionId = payload.reference
                )
            } else {
                // Fallback for freespin or zero-win (saga skipped wallet operation)
                val balance = walletAdapter.findBalance(session.playerId, session.currency).getOrElse {
                    return@profileSuspend it.toErrorResponse()
                }
                PragmaticResponse.Success(
                    cash = currencyAdapter.convertSystemToProvider(balance.real, balance.currency).toString(),
                    bonus = currencyAdapter.convertSystemToProvider(balance.bonus, balance.currency).toString(),
                    currency = balance.currency.value,
                    transactionId = payload.reference
                )
            }
        }

    suspend fun endRound(sessionToken: SessionToken, roundId: String): PragmaticResponse =
        Logger.profileSuspend("Pragmatic.endRound(round=$roundId)") {
            val session = sessionService.findByToken(sessionToken).getOrElse {
                Logger.error("[Pragmatic] endRound failed: session lookup error for round=$roundId: ${it.message}")
                return@profileSuspend it.toErrorResponse()
            }

            val context = EndSpinContext(
                session = session,
                extRoundId = roundId,
                freeSpinId = null
            )

            endSpinSaga.execute(context).getOrElse {
                Logger.error("[Pragmatic] endRound saga failed for round=$roundId player=${session.playerId}: ${it.message}")
                return@profileSuspend it.toErrorResponse()
            }

            balance(sessionToken)
        }

    suspend fun refund(sessionToken: SessionToken, roundId: String, transactionId: String): PragmaticResponse =
        Logger.profileSuspend("Pragmatic.refund(round=$roundId)") {
            val session = sessionService.findByToken(sessionToken).getOrElse {
                Logger.error("[Pragmatic] refund failed: session lookup error for round=$roundId: ${it.message}")
                return@profileSuspend it.toErrorResponse()
            }

            val context = RollbackSpinContext(
                session = session,
                extRoundId = roundId,
                transactionId = transactionId
            )

            rollbackSpinSaga.execute(context).getOrElse {
                Logger.error("[Pragmatic] refund saga failed for round=$roundId player=${session.playerId}: ${it.message}")
                return@profileSuspend it.toErrorResponse()
            }

            // Use cached balance from saga (no extra HTTP call!)
            val currentBalance = context.resultBalance
            if (currentBalance != null) {
                PragmaticResponse.Success(
                    cash = currencyAdapter.convertSystemToProvider(currentBalance.real, currentBalance.currency).toString(),
                    bonus = currencyAdapter.convertSystemToProvider(currentBalance.bonus, currentBalance.currency).toString(),
                    currency = currentBalance.currency.value
                )
            } else {
                // Fallback for freespin or no refund needed
                balance(sessionToken)
            }
        }

    suspend fun adjustment(
        sessionToken: SessionToken,
        roundId: String,
        reference: String,
        amount: String
    ): PragmaticResponse {
        val session = sessionService.findByToken(sessionToken).getOrElse {
            Logger.error("[Pragmatic] adjustment failed: session lookup error for round=$roundId: ${it.message}")
            return it.toErrorResponse()
        }

        val realAmount = amount.toBigDecimal().let {
            currencyAdapter.convertProviderToSystem(it, session.currency)
        }

        val game = gameService.findById(session.gameId).getOrElse {
            Logger.error("[Pragmatic] adjustment failed: game lookup error for gameId=${session.gameId}: ${it.message}")
            return it.toErrorResponse()
        }

        val resultBalance = if (realAmount < 0L) {
            val betAmount = -realAmount

            val context = PlaceSpinContext(
                session = session,
                gameSymbol = game.symbol,
                extRoundId = roundId,
                transactionId = reference,
                freeSpinId = null,
                amount = betAmount
            )

            placeSpinSaga.execute(context).getOrElse {
                Logger.error("[Pragmatic] adjustment place saga failed for round=$roundId player=${session.playerId}: ${it.message}")
                return it.toErrorResponse()
            }

            context.resultBalance
        } else {
            val context = SettleSpinContext(
                session = session,
                extRoundId = roundId,
                transactionId = reference,
                freeSpinId = null,
                winAmount = realAmount
            )

            settleSpinSaga.execute(context).getOrElse {
                Logger.error("[Pragmatic] adjustment settle saga failed for round=$roundId player=${session.playerId}: ${it.message}")
                return it.toErrorResponse()
            }

            context.resultBalance
        }

        // Use cached balance from saga (no extra HTTP call!)
        val balance = resultBalance ?: walletAdapter.findBalance(session.playerId, session.currency).getOrElse {
            Logger.error("[Pragmatic] adjustment failed: balance lookup error for player=${session.playerId}: ${it.message}")
            return it.toErrorResponse()
        }

        return PragmaticResponse.Success(
            cash = currencyAdapter.convertSystemToProvider(balance.real, balance.currency).toString(),
            bonus = currencyAdapter.convertSystemToProvider(balance.bonus, balance.currency).toString(),
            currency = balance.currency.value,
        )
    }

    private fun Throwable.toErrorResponse(): PragmaticResponse {
        TODO("Not yet implemented")
    }
}
