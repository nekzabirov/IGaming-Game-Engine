package domain.model

import domain.exception.conflict.CasinoRoundAlreadyFinishedException
import domain.exception.domainRequire
import domain.util.ext.InstantExt
import domain.vo.Currency
import domain.vo.ExternalCasinoRoundId
import domain.vo.FreespinId
import domain.vo.PlayerId
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * The hub names a round by its own [externalId] directly — no session in between. Two legs of one
 * round arriving at once resolve to the SAME row through a unique index on [externalId], not a
 * check in code.
 *
 * [game] is null for a sportsbook leg — the hub's wallet contract carries an empty `game` for
 * those, and that single flag is what tells `WalletGrpcService` apart from a casino spin. There is
 * deliberately no separate Bet aggregate any more: money is money, and the wallet path is now the
 * same for both products.
 */
@Serializable
data class CasinoRound(
    val id: Long = Long.MIN_VALUE,

    val externalId: ExternalCasinoRoundId,

    val freespinId: FreespinId? = null,

    val playerId: PlayerId,

    val game: CasinoGame?,

    val currency: Currency,

    val createdAt: Instant = InstantExt.now(),

    val finishedAt: Instant? = null,
) {
    val isFinished: Boolean
        get() = finishedAt != null

    /**
     * Closes the round and returns the finished [CasinoRound]. The usecase publishes a
     * `CasinoRoundEvent` snapshot after persistence commits.
     *
     * Throws [CasinoRoundAlreadyFinishedException] if the round was already closed.
     */
    fun finish(): CasinoRound {
        domainRequire(!isFinished) { CasinoRoundAlreadyFinishedException() }
        return copy(finishedAt = InstantExt.now())
    }
}
