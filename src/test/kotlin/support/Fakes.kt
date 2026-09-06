package support

import clients.Balance
import clients.GameHub
import clients.PlayerLimits
import clients.Wallet
import db.Platform
import events.AppEvent
import events.EventPublisher
import gamehub.v1.Gateway
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

/** A wallet that keeps balances in memory and is idempotent by reference, like pam. */
class FakeWallet(vararg initial: Pair<Pair<String, String>, Balance>) : Wallet {

    data class Move(val playerId: String, val reference: String, val currency: String, val real: Long, val bonus: Long)

    val balances = mutableMapOf(*initial)

    val moves = mutableListOf<Move>()

    val reads = mutableListOf<Pair<String, String>>()

    var failNextMove: Throwable? = null

    override suspend fun balance(playerId: String, currency: String): Balance {
        reads += playerId to currency
        return balances.getOrPut(playerId to currency) { Balance(0, 0, currency) }
    }

    override suspend fun transact(playerId: String, reference: String, currency: String, realAmount: Long, bonusAmount: Long): Balance {
        failNextMove?.let { failNextMove = null; throw it }
        // Like pam's adapter: a movement of nothing is a balance read, never a ledger entry.
        if (realAmount == 0L && bonusAmount == 0L) return balance(playerId, currency)
        if (moves.any { it.reference == reference }) return balances.getValue(playerId to currency)
        moves += Move(playerId, reference, currency, realAmount, bonusAmount)
        val current = balances.getOrPut(playerId to currency) { Balance(0, 0, currency) }
        val next = Balance(current.real + realAmount, current.bonus + bonusAmount, currency)
        balances[playerId to currency] = next
        return next
    }
}

class FakeLimits : PlayerLimits {

    val store = mutableMapOf<String, Long>()

    override suspend fun maxPlace(playerId: String): Long? = store[playerId]

    override suspend fun setMaxPlace(playerId: String, amount: Long) {
        store[playerId] = amount
    }
}

class RecordingEvents : EventPublisher {

    val events = mutableListOf<AppEvent>()

    var failNext: Throwable? = null

    override suspend fun publish(event: AppEvent) {
        failNext?.let { failNext = null; throw it }
        events += event
    }

    fun last(): JsonObject = events.last().data().jsonObject

    fun <T : AppEvent> only(type: Class<T>): T = events.filterIsInstance(type).single()
}

class FakeGameHub(var catalog: Gateway.ListCasinoResponse = Gateway.ListCasinoResponse.getDefaultInstance()) : GameHub {

    data class Launch(val game: String, val playerId: String?, val currency: String, val locale: String, val platform: Platform, val freespinId: String?)

    val launches = mutableListOf<Launch>()

    var launchFailure: Throwable? = null

    override suspend fun listCasino(): Gateway.ListCasinoResponse = catalog

    override suspend fun launchCasino(
        game: String,
        playerId: String,
        currency: String,
        locale: String,
        lobbyUrl: String,
        platform: Platform,
        freespinId: String?,
    ): String {
        launchFailure?.let { throw it }
        launches += Launch(game, playerId, currency, locale, platform, freespinId)
        return "https://vendor/launch/$game?player=$playerId"
    }

    override suspend fun launchCasinoDemo(game: String, currency: String, locale: String, lobbyUrl: String, platform: Platform): String {
        launches += Launch(game, null, currency, locale, platform, null)
        return "https://vendor/demo/$game"
    }

    override suspend fun freespinPresets(game: String): Gateway.FreespinPresetsResponse =
        Gateway.FreespinPresetsResponse.newBuilder().setMinAmount(1000).putPresets("paylines", "20").build()

    override suspend fun createFreespin(
        game: String,
        playerId: String,
        amount: Long,
        count: Int,
        currency: String,
        presets: Map<String, String>,
        reference: String,
        durationSeconds: Long,
    ) = Unit

    override suspend fun cancelFreespin(reference: String) = Unit

    override suspend fun openSportbook(playerId: String?, currency: String, locale: String): Gateway.OpenSportbookResponse =
        Gateway.OpenSportbookResponse.newBuilder().setIntegration("01TECHSPORT").putData("currency", currency).build()
}
