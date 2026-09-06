package services

import clients.Balance
import clients.PlayerLimits
import clients.Wallet
import db.CasinoGames
import db.CasinoRound
import db.CasinoRounds
import db.Spin
import db.SpinType
import db.Spins
import errors.CasinoRoundAlreadyFinishedException
import errors.CasinoRoundNotFoundException
import errors.MaxPlaceSpinException
import errors.SpinAlreadyExistsException
import events.AppEvent
import events.EventPublisher
import events.RoundEvent
import events.RoundPayload
import events.SpinEvent
import events.SpinPayload
import events.toPayload
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.datetime.Clock
import org.jetbrains.exposed.dao.flushCache
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.upsert
import org.slf4j.LoggerFactory
import plugins.dbTransaction
import java.sql.SQLException
import kotlin.coroutines.cancellation.CancellationException

/**
 * The money path — what the hub calls for every leg of every round, casino and sportsbook alike.
 *
 * Every movement is three steps, in this order, never inside one transaction:
 *   1. a DB transaction that finds the leg (replay) or opens the round and snapshots what the
 *      money needs — the entities never leave it, plain values do;
 *   2. the wallet: balance, limit, split, then ONE movement keyed `spin:<type>:<leg id>`, on which
 *      pam is idempotent;
 *   3. a DB transaction that writes the spin row — the unique index on the leg id is what decides
 *      a race between two redeliveries — and the event, published once the row is committed.
 *
 * A wallet failure leaves the row unwritten and the call answers INTERNAL: the hub retries under
 * the same leg id and the money still moves at most once. A broker failure after the wallet moved
 * is logged, never surfaced — the hub would read it as "unknown outcome" and retry a movement that
 * landed.
 */
class WalletService(
    private val wallet: Wallet,
    private val limits: PlayerLimits,
    private val events: EventPublisher,
    private val freespinToPayout: Boolean,
) {

    private val log = LoggerFactory.getLogger(WalletService::class.java)

    /** One leg as the hub names it. [game] null means a sportsbook leg. */
    data class Leg(
        val id: String,
        val roundId: String,
        val playerId: String,
        val game: String?,
        val amount: Long,
        val currency: String,
        val freespinId: String?,
    )

    private class RoundRef(
        val id: Long,
        val playerId: String,
        val currency: String,
        val freespinId: String?,
        val bonusBetEnabled: Boolean,
        val payload: RoundPayload,
    )

    private class SpinRef(
        val id: Long,
        val type: SpinType,
        val realAmount: Long,
        val bonusAmount: Long,
        val payload: SpinPayload,
    )

    private class Draft(
        val externalId: String,
        val type: SpinType,
        val amount: Long,
        val round: RoundRef,
        val reference: SpinRef?,
    )

    suspend fun balance(playerId: String, currency: String): Balance = wallet.balance(playerId, currency)

    suspend fun place(leg: Leg): Balance = movement(leg, SpinType.PLACE)

    /** A credit can be the first thing heard about a round (a win-only bonus payout): it opens it too. */
    suspend fun settle(leg: Leg): Balance = movement(leg, SpinType.SETTLE)

    /**
     * Reverses ONE committed leg by its id. A leg that matches nothing answers success with no
     * balance: the hub walks both legs of a transaction in one pass and stops at the first refusal,
     * so an unknown win leg would otherwise keep the bet behind it from ever being given back — and
     * with no leg there is no player whose balance could honestly be reported.
     */
    suspend fun rollback(legId: String): Balance? {
        val rollbackId = "$legId$ROLLBACK_SUFFIX"

        val draft = dbTransaction {
            val original = Spin.findByExternalId(legId) ?: return@dbTransaction null
            val round = original.round
            if (Spin.findByExternalId(rollbackId) != null) {
                return@dbTransaction Draft(rollbackId, SpinType.ROLLBACK, original.amount, round.ref(), null)
            }
            Draft(rollbackId, SpinType.ROLLBACK, original.amount, round.ref(), original.ref())
        } ?: return null

        // The rollback id derives from the leg it reverses, so a redelivery is cheap to detect.
        if (draft.reference == null) return wallet.balance(draft.round.playerId, draft.round.currency)

        return try {
            process(draft)
        } catch (_: SpinAlreadyExistsException) {
            wallet.balance(draft.round.playerId, draft.round.currency)
        }
    }

    /** Closes the round and publishes it finished. Moves no money. */
    suspend fun closeRound(externalRoundId: String) {
        val payload = dbTransaction {
            val round = CasinoRound.findByExternalId(externalRoundId) ?: throw CasinoRoundNotFoundException()
            if (round.isFinished) throw CasinoRoundAlreadyFinishedException()
            round.finishedAt = Clock.System.now()
            round.toPayload()
        }

        publishQuietly(RoundEvent(payload), "roundId=${payload.id}")

        log.info("CasinoRound finished: id={}", payload.id)
    }

    private suspend fun movement(leg: Leg, type: SpinType): Balance {
        val draft = dbTransaction {
            // Replay guard: the hub redelivers on timeout, so this leg may already be committed.
            // Answer success with the current balance — no second row, no second wallet move.
            if (Spin.findByExternalId(leg.id) != null) return@dbTransaction null

            val round = openRound(leg)
            val reference = if (type == SpinType.SETTLE) Spin.findBonusPlace(round.id.value)?.ref() else null

            Draft(leg.id, type, leg.amount, round.ref(), reference)
        } ?: return wallet.balance(leg.playerId, leg.currency)

        // The lookup above cannot see a redelivery still in flight; the unique constraint is what
        // decides the winner, and the loser answers exactly as the lookup would have.
        return try {
            process(draft)
        } catch (_: SpinAlreadyExistsException) {
            wallet.balance(leg.playerId, leg.currency)
        }
    }

    /**
     * The round the hub names, opened on first sight. Two legs of a brand-new round arriving at
     * once resolve to one row through the unique index, not a check in code. A finished round is
     * reopened: a late correction legitimately lands hours after CloseRound, and closing already
     * published its event, so nothing is published here.
     */
    private fun openRound(leg: Leg): CasinoRound {
        CasinoRound.findByExternalId(leg.roundId)?.let { round ->
            if (round.isFinished) round.finishedAt = null
            return round
        }

        // An unknown game is NOT refused: the round opens without one, like a sportsbook leg. The
        // alternative is answering INTERNAL — the one code that starts a vendor rollback cycle.
        val gameId = leg.game?.let { identity ->
            CasinoGames.select(CasinoGames.id).where { CasinoGames.identity eq identity }.singleOrNull()?.get(CasinoGames.id)
        }

        val row = CasinoRounds.upsert(
            CasinoRounds.externalId,
            onUpdate = { it[CasinoRounds.finishedAt] = null },
        ) {
            it[externalId] = leg.roundId
            it[freespinId] = leg.freespinId
            it[playerId] = leg.playerId
            it[game] = gameId
            it[currency] = leg.currency
            it[createdAt] = Clock.System.now()
        }.resultedValues!!.single()

        return CasinoRound.wrapRow(row)
    }

    /**
     * A free round's BET costs nothing, so it neither checks nor moves the balance. Its WINNINGS are
     * ordinary money with [freespinToPayout]; without it the whole round stays off the wallet and the
     * promotion's owner settles it. The row still carries the full amount with a zero real/bonus split
     * — which is what says "no money moved here" to everything downstream, the event included.
     */
    private suspend fun process(draft: Draft): Balance {
        val round = draft.round
        val offWallet = round.freespinId != null && (draft.type == SpinType.PLACE || !freespinToPayout)

        val (split, balance) = if (offWallet) {
            SpinMath.Split(draft.amount, 0, 0) to wallet.balance(round.playerId, round.currency)
        } else {
            move(draft)
        }

        val payload = dbTransaction { insert(draft, split) }

        publishQuietly(SpinEvent(payload), "spinId=${payload.id} externalId=${draft.externalId} type=${draft.type}")

        log.info("Spin processed: id={} type={} round={}", payload.id, draft.type, round.id)

        return balance
    }

    private suspend fun move(draft: Draft): Pair<SpinMath.Split, Balance> = coroutineScope {
        val split = async { split(draft) }

        checkLimit(draft)

        val result = split.await()

        // PLACE takes money, SETTLE gives it back; a ROLLBACK moves opposite to the leg it reverses.
        val takesMoney = draft.type == SpinType.PLACE ||
            (draft.type == SpinType.ROLLBACK && draft.reference?.type == SpinType.SETTLE)
        val sign = if (takesMoney) -1 else 1

        // Awaited before the row is written, and NOT swallowed on failure: nothing here reconciles a
        // movement that did not land. The answer is the wallet's, not the calculator's projection.
        val balance = wallet.transact(
            playerId = draft.round.playerId,
            reference = "spin:${draft.type.name.lowercase()}:${draft.externalId}",
            currency = draft.round.currency,
            realAmount = sign * result.real,
            bonusAmount = sign * result.bonus,
        )

        result to balance
    }

    private suspend fun split(draft: Draft): SpinMath.Split = when (draft.type) {
        SpinType.PLACE -> SpinMath.place(
            balance = wallet.balance(draft.round.playerId, draft.round.currency),
            amount = draft.amount,
            bonusBetEnabled = draft.round.bonusBetEnabled,
        )

        SpinType.SETTLE -> SpinMath.settle(draft.amount, draft.reference?.bonusAmount ?: 0)

        SpinType.ROLLBACK -> {
            val reference = checkNotNull(draft.reference) { "a rollback always names the leg it reverses" }
            if (reference.type == SpinType.SETTLE) {
                SpinMath.reclaim(wallet.balance(draft.round.playerId, draft.round.currency), reference.realAmount, reference.bonusAmount)
            } else {
                SpinMath.refund(reference.realAmount, reference.bonusAmount)
            }
        }
    }

    private suspend fun checkLimit(draft: Draft) {
        if (draft.type != SpinType.PLACE) return
        val maxPlace = limits.maxPlace(draft.round.playerId) ?: return
        if (maxPlace <= draft.amount) throw MaxPlaceSpinException()
    }

    private fun org.jetbrains.exposed.sql.Transaction.insert(draft: Draft, split: SpinMath.Split): SpinPayload {
        val spin = Spin.new {
            externalId = draft.externalId
            roundId = EntityID(draft.round.id, CasinoRounds)
            referenceId = draft.reference?.let { EntityID(it.id, Spins) }
            type = draft.type
            amount = split.amount
            realAmount = split.real
            bonusAmount = split.bonus
        }

        try {
            flushCache()
        } catch (e: ExposedSQLException) {
            if (e.isUniqueViolation()) throw SpinAlreadyExistsException() else throw e
        }

        return SpinPayload(
            id = spin.id.value,
            externalId = draft.externalId,
            round = draft.round.payload,
            reference = draft.reference?.payload,
            type = draft.type,
            amount = split.amount,
            realAmount = split.real,
            bonusAmount = split.bonus,
        )
    }

    private fun CasinoRound.ref() = RoundRef(
        id = id.value,
        playerId = playerId,
        currency = currency,
        freespinId = freespinId,
        // Null game is a sportsbook leg: no per-game flag to gate it, so bonus betting is allowed
        // by default, the same as a freshly synced game.
        bonusBetEnabled = game?.bonusBetEnable ?: true,
        payload = toPayload(),
    )

    private fun Spin.ref() = SpinRef(
        id = id.value,
        type = type,
        realAmount = realAmount,
        bonusAmount = bonusAmount,
        payload = toPayload(),
    )

    private suspend fun publishQuietly(event: AppEvent, what: String) {
        try {
            events.publish(event)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.error("EVENT PUBLISH FAILED (event lost): route={} {}", event.route, what, e)
        }
    }

    /** Postgres `unique_violation`; the driver exposes it as the SQLState, not as a typed error. */
    private fun ExposedSQLException.isUniqueViolation(): Boolean =
        generateSequence<Throwable>(this) { it.cause }
            .filterIsInstance<SQLException>()
            .any { it.sqlState == UNIQUE_VIOLATION_SQL_STATE }

    private companion object {
        const val ROLLBACK_SUFFIX = ":rollback"

        const val UNIQUE_VIOLATION_SQL_STATE = "23505"
    }
}
