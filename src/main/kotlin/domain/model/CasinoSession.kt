package domain.model

import domain.util.ext.InstantExt
import domain.vo.Currency
import domain.vo.Locale
import domain.vo.PlayerId
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * A record that a player opened a game — nothing more. The hub owns the actual session (its own
 * token, its own launch bookkeeping); this value exists only so `OpenCasinoSessionUsecase` has
 * something to publish on `session.events` for crm-engine. It is never persisted and never looked
 * up — wallet calls from the hub carry `playerId`/`game`/`currency` directly, with no session
 * indirection at all.
 */
@Serializable
data class CasinoSession(
    val playerId: PlayerId,

    val game: CasinoGame,

    val currency: Currency,

    val locale: Locale,

    val platform: Platform,

    val createdAt: Instant = InstantExt.now(),
)
