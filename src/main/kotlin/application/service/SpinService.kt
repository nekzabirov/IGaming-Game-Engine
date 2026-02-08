package application.service

import application.port.outbound.PlayerAdapter
import application.port.outbound.RoundRepository
import application.port.outbound.SpinRepository
import application.port.outbound.external.WalletAdapter
import domain.common.error.*
import domain.common.value.SpinType
import domain.game.model.Game
import domain.session.model.Session
import domain.session.model.Spin
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import shared.Logger
import java.util.UUID

/**
 * Command object for placing/settling spins.
 */
data class SpinCommand(
    val extRoundId: String,
    val transactionId: String,
    val amount: Long,
    val freeSpinId: String? = null
) {
    class Builder {
        private var extRoundId: String = ""
        private var transactionId: String = ""
        private var amount: Long = 0L
        private var freeSpinId: String? = null

        fun extRoundId(value: String) = apply { extRoundId = value }
        fun transactionId(value: String) = apply { transactionId = value }
        fun amount(value: Long) = apply { amount = value }
        fun freeSpinId(value: String?) = apply { freeSpinId = value }

        fun build() = SpinCommand(extRoundId, transactionId, amount, freeSpinId)
    }

    companion object {
        fun builder() = Builder()
    }
}

/**
 * Application service for spin-related operations.
 * Uses constructor injection for all dependencies.
 */
class SpinService(
    private val walletAdapter: WalletAdapter,
    private val playerAdapter: PlayerAdapter,
    private val roundRepository: RoundRepository,
    private val spinRepository: SpinRepository
) {
    /**
     * Place a spin (bet).
     * If freeSpinId is provided, skip wallet operations (freespin mode).
     */
    suspend fun place(session: Session, game: Game, command: SpinCommand): Result<Unit> {
        val isFreeSpin = command.freeSpinId != null

        // Create or get round
        val round = roundRepository.findOrCreate(session.id, game.id, command.extRoundId)

        if (isFreeSpin) {
            // FreeSpin mode: just save to DB, no wallet operations
            val spin = Spin(
                id = UUID.randomUUID(),
                roundId = round.id,
                type = SpinType.PLACE,
                amount = command.amount,
                realAmount = 0L,
                bonusAmount = 0L,
                extId = command.transactionId,
                freeSpinId = command.freeSpinId
            )

            spinRepository.save(spin)
            return Result.success(Unit)
        }

        // Normal mode: use wallet
        // Fetch balance and bet limit in parallel
        val (balance, betLimit) = coroutineScope {
            val balanceDeferred = async { walletAdapter.findBalance(session.playerId, session.currency) }
            val betLimitDeferred = async { playerAdapter.findCurrentBetLimit(session.playerId) }

            val balanceResult = balanceDeferred.await().getOrElse {
                return@coroutineScope Result.failure(it)
            }
            val betLimitResult = betLimitDeferred.await().getOrElse {
                return@coroutineScope Result.failure(it)
            }

            // Adjust balance if bonus bet is disabled
            val adjustedBalance = if (!game.bonusBetEnable) {
                balanceResult.copy(bonus = 0L)
            } else {
                balanceResult
            }

            Result.success(adjustedBalance to betLimitResult)
        }.getOrElse { return Result.failure(it) }

        // Validate bet limit
        if (betLimit != null && betLimit < command.amount) {
            Logger.warn("[SpinService] place: bet limit exceeded for player=${session.playerId} amount=${command.amount} limit=$betLimit")
            return Result.failure(
                BetLimitExceededError(session.playerId, command.amount, betLimit)
            )
        }

        // Validate sufficient balance
        if (command.amount > balance.totalAmount) {
            Logger.warn("[SpinService] place: insufficient balance for player=${session.playerId} required=${command.amount} available=${balance.totalAmount}")
            return Result.failure(
                InsufficientBalanceError(session.playerId, command.amount, balance.totalAmount)
            )
        }

        // Calculate real and bonus amounts
        val betRealAmount = minOf(command.amount, balance.real)
        val betBonusAmount = command.amount - betRealAmount

        // Create spin record
        val spin = Spin(
            id = UUID.randomUUID(),
            roundId = round.id,
            type = SpinType.PLACE,
            amount = command.amount,
            realAmount = betRealAmount,
            bonusAmount = betBonusAmount,
            extId = command.transactionId
        ).let { spinRepository.save(it) }

        // Withdraw from wallet
        walletAdapter.withdraw(
            playerId = session.playerId,
            transactionId = spin.id.toString(),
            currency = session.currency,
            realAmount = betRealAmount,
            bonusAmount = betBonusAmount
        ).getOrElse {
            Logger.error("[SpinService] place: wallet withdraw failed for player=${session.playerId} tx=${spin.id}: ${it.message}")
            return Result.failure(it)
        }

        return Result.success(Unit)
    }

    /**
     * Settle a spin (determine win/loss).
     * If freeSpinId is provided, skip wallet operations (freespin mode).
     */
    suspend fun settle(session: Session, extRoundId: String, command: SpinCommand): Result<Unit> {
        val isFreeSpin = command.freeSpinId != null

        // Find the round
        val round = roundRepository.findBySessionAndExtId(session.id, extRoundId)
            ?: run {
                Logger.warn("[SpinService] settle: round not found extRoundId=$extRoundId session=${session.id}")
                return Result.failure(RoundNotFoundError(extRoundId))
            }

        // Find the place spin
        val placeSpin = spinRepository.findByRoundAndType(round.id, SpinType.PLACE)
            ?: run {
                Logger.warn("[SpinService] settle: place spin not found for round=$extRoundId")
                return Result.failure(RoundFinishedError(extRoundId))
            }

        if (isFreeSpin) {
            // FreeSpin mode: just save to DB, no wallet operations
            val settleSpin = Spin(
                id = UUID.randomUUID(),
                roundId = round.id,
                type = SpinType.SETTLE,
                amount = command.amount,
                realAmount = 0L,
                bonusAmount = 0L,
                extId = command.transactionId,
                referenceId = placeSpin.id,
                freeSpinId = command.freeSpinId
            )

            spinRepository.save(settleSpin)
            return Result.success(Unit)
        }

        // Normal mode: use wallet
        // Determine if bonus was used
        val isBonusUsed = placeSpin.bonusAmount > 0L

        // Calculate win amounts
        val realAmount = if (isBonusUsed) 0L else command.amount
        val bonusAmount = if (isBonusUsed) command.amount else 0L

        // Create settle spin
        val settleSpin = Spin(
            id = UUID.randomUUID(),
            roundId = round.id,
            type = SpinType.SETTLE,
            amount = command.amount,
            realAmount = realAmount,
            bonusAmount = bonusAmount,
            extId = command.transactionId,
            referenceId = placeSpin.id
        ).let { spinRepository.save(it) }

        // Deposit winnings
        walletAdapter.deposit(
            playerId = session.playerId,
            transactionId = settleSpin.id.toString(),
            currency = session.currency,
            realAmount = realAmount,
            bonusAmount = bonusAmount
        ).getOrElse {
            Logger.error("[SpinService] settle: wallet deposit failed for player=${session.playerId} tx=${settleSpin.id}: ${it.message}")
            return Result.failure(it)
        }

        return Result.success(Unit)
    }

    /**
     * Rollback a spin.
     * If freeSpinId is provided, skip wallet operations (freespin mode).
     */
    suspend fun rollback(session: Session, command: SpinCommand): Result<Unit> {
        // Find the round
        val round = roundRepository.findBySessionAndExtId(session.id, command.extRoundId)
            ?: run {
                Logger.warn("[SpinService] rollback: round not found extRoundId=${command.extRoundId} session=${session.id}")
                return Result.failure(RoundNotFoundError(command.extRoundId))
            }

        // Find the spin to rollback
        val spin = spinRepository.findAllByRound(round.id).firstOrNull()
            ?: run {
                Logger.warn("[SpinService] rollback: no spin found for round=${command.extRoundId}")
                return Result.failure(RoundNotFoundError(command.extRoundId))
            }

        val isFreeSpin = spin.freeSpinId != null

        // Create rollback spin
        val rollbackSpin = Spin(
            id = UUID.randomUUID(),
            roundId = round.id,
            type = SpinType.ROLLBACK,
            amount = 0L,
            realAmount = 0L,
            bonusAmount = 0L,
            extId = command.transactionId,
            referenceId = spin.id,
            freeSpinId = command.freeSpinId
        )

        spinRepository.save(rollbackSpin)

        if (!isFreeSpin) {
            // Normal mode: rollback in wallet
            walletAdapter.rollback(session.playerId, spin.id.toString())
        }

        return Result.success(Unit)
    }

    /**
     * Close a round.
     */
    suspend fun closeRound(session: Session, extRoundId: String): Result<Unit> {
        val round = roundRepository.findBySessionAndExtId(session.id, extRoundId)
            ?: run {
                Logger.warn("[SpinService] closeRound: round not found extRoundId=$extRoundId session=${session.id}")
                return Result.failure(RoundNotFoundError(extRoundId))
            }

        roundRepository.finish(round.id)

        return Result.success(Unit)
    }
}
