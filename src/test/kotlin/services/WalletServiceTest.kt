package services

import clients.Balance
import db.CasinoRound
import db.CasinoRounds
import db.Spin
import db.SpinType
import db.Spins
import errors.CasinoRoundAlreadyFinishedException
import errors.InsufficientBalanceException
import errors.MaxPlaceSpinException
import events.RoundEvent
import events.SpinEvent
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.sql.selectAll
import plugins.dbRead
import support.FakeLimits
import support.FakeWallet
import support.Fixtures
import support.RecordingEvents
import support.TestDatabase

/**
 * The money path against a real Postgres: the unique index on `spins.external_id` and the
 * `casino_rounds.external_id` upsert are the guarantees under test, not a fake of them.
 */
class WalletServiceTest : FunSpec({

    TestDatabase.connect()

    lateinit var wallet: FakeWallet
    lateinit var limits: FakeLimits
    lateinit var events: RecordingEvents

    fun service(freespinToPayout: Boolean = true) = WalletService(wallet, limits, events, freespinToPayout)

    fun leg(
        id: String,
        roundId: String = "round-1",
        game: String? = "gates_of_olympus",
        amount: Long = 100,
        freespinId: String? = null,
    ) = WalletService.Leg(id, roundId, "1", game, amount, "UAH", freespinId)

    beforeTest {
        TestDatabase.reset()
        wallet = FakeWallet(("1" to "UAH") to Balance(1000, 500, "UAH"))
        limits = FakeLimits()
        events = RecordingEvents()
        val provider = Fixtures.provider("pragmatic")
        Fixtures.game("gates_of_olympus", provider, tags = listOf("slots"))
        Fixtures.game("real_only", provider, bonusBetEnable = false)
    }

    test("PLACE takes the bet from real first, writes the leg and publishes it after the wallet moved") {
        val balance = service().place(leg("leg-1"))

        balance shouldBe Balance(900, 500, "UAH")
        wallet.moves.single() shouldBe FakeWallet.Move("1", "spin:place:leg-1", "UAH", -100, 0)

        val spin = dbRead { Spin.findByExternalId("leg-1")!!.also { it.round } }
        spin.type shouldBe SpinType.PLACE
        spin.realAmount shouldBe 100
        spin.bonusAmount shouldBe 0

        val event = events.only(SpinEvent::class.java)
        event.playerId shouldBe "1"
        val data = event.data().jsonObject
        data["type"]!!.jsonPrimitive.content shouldBe "PLACE"
        data["gameIdentity"]!!.jsonPrimitive.content shouldBe "gates_of_olympus"
        data["gameProvider"]!!.jsonPrimitive.content shouldBe "pragmatic"
        data["currency"]!!.jsonPrimitive.content shouldBe "UAH"
        data.containsKey("freespinId") shouldBe false
        data["round"]!!.jsonObject["session"]!!.jsonObject["playerId"]!!.jsonPrimitive.content shouldBe "1"
        data["round"]!!.jsonObject["gameVariant"]!!.jsonObject["game"]!!.jsonObject["provider"]!!.jsonObject["identity"]!!.jsonPrimitive.content shouldBe "pragmatic"
        data["round"]!!.jsonObject["game"]!!.jsonObject["tags"].toString() shouldBe "[\"slots\"]"
        data["reference"].toString() shouldBe "null"
    }

    test("a redelivered PLACE moves nothing and answers the current balance") {
        service().place(leg("leg-1"))
        val again = service().place(leg("leg-1"))

        again shouldBe Balance(900, 500, "UAH")
        wallet.moves.size shouldBe 1
        dbRead { Spin.count() } shouldBe 1
        events.events.size shouldBe 1
    }

    test("a bet the balance cannot cover is refused before anything is written") {
        shouldThrow<InsufficientBalanceException> { service().place(leg("leg-big", amount = 5000)) }

        wallet.moves shouldBe emptyList()
        dbRead { Spin.count() } shouldBe 0
        events.events shouldBe emptyList()
        // The round was opened by the first leg heard — that is fine, the spin is what was refused.
        dbRead { CasinoRound.findByExternalId("round-1") }.shouldNotBeNull()
    }

    test("a bet over the player's cap is refused; the cap must be strictly above the bet") {
        limits.store["1"] = 100
        shouldThrow<MaxPlaceSpinException> { service().place(leg("leg-1", amount = 100)) }

        limits.store["1"] = 101
        service().place(leg("leg-1", amount = 100))
        wallet.moves.size shouldBe 1
    }

    test("a game that forbids bonus betting takes real only, and refuses when real is short") {
        wallet.balances["1" to "UAH"] = Balance(50, 500, "UAH")
        shouldThrow<InsufficientBalanceException> { service().place(leg("leg-1", game = "real_only")) }

        wallet.balances["1" to "UAH"] = Balance(150, 500, "UAH")
        service().place(leg("leg-2", game = "real_only")) shouldBe Balance(50, 500, "UAH")
    }

    test("a win of a bonus-funded round pays the bonus pool and references the bet") {
        wallet.balances["1" to "UAH"] = Balance(30, 500, "UAH")
        service().place(leg("leg-1", amount = 100))
        wallet.moves.single().let { it.real shouldBe -30; it.bonus shouldBe -70 }

        service().settle(leg("leg-2", amount = 250)) shouldBe Balance(0, 680, "UAH")
        wallet.moves.last() shouldBe FakeWallet.Move("1", "spin:settle:leg-2", "UAH", 0, 250)

        val (settle, reference) = dbRead { Spin.findByExternalId("leg-2")!!.let { it to it.reference?.externalId } }
        settle.bonusAmount shouldBe 250
        reference shouldBe "leg-1"
        events.last()["reference"]!!.jsonObject["externalId"]!!.jsonPrimitive.content shouldBe "leg-1"
    }

    test("a win can be the first thing heard about a round: it opens it and pays real") {
        service().settle(leg("win-only", roundId = "round-9", amount = 40)) shouldBe Balance(1040, 500, "UAH")

        dbRead { CasinoRound.findByExternalId("round-9") }.shouldNotBeNull()
        wallet.moves.single().reference shouldBe "spin:settle:win-only"
    }

    test("a losing round settles as a zero move and still gets its row and event") {
        service().settle(leg("leg-0", amount = 0)) shouldBe Balance(1000, 500, "UAH")

        wallet.moves shouldBe emptyList()
        dbRead { Spin.findByExternalId("leg-0")!!.amount } shouldBe 0
        events.events.size shouldBe 1
    }

    test("rolling back a bet refunds the original split, and a repeat moves nothing") {
        wallet.balances["1" to "UAH"] = Balance(30, 500, "UAH")
        service().place(leg("leg-1", amount = 100))

        service().rollback("leg-1") shouldBe Balance(30, 500, "UAH")
        wallet.moves.last() shouldBe FakeWallet.Move("1", "spin:rollback:leg-1:rollback", "UAH", 30, 70)
        dbRead { Spin.findByExternalId("leg-1:rollback")!!.let { it.type to it.reference?.externalId } } shouldBe (SpinType.ROLLBACK to "leg-1")

        service().rollback("leg-1") shouldBe Balance(30, 500, "UAH")
        wallet.moves.size shouldBe 2
        events.events.size shouldBe 2
    }

    test("rolling back a win takes it back, clamped to what is left") {
        service().settle(leg("win", amount = 400))
        wallet.balances["1" to "UAH"] = Balance(150, 0, "UAH")

        service().rollback("win") shouldBe Balance(0, 0, "UAH")
        wallet.moves.last() shouldBe FakeWallet.Move("1", "spin:rollback:win:rollback", "UAH", -150, 0)
        dbRead { Spin.findByExternalId("win:rollback")!!.amount } shouldBe 150
    }

    test("a rollback of a leg never seen answers success with no balance and touches nothing") {
        service().rollback("leg-we-never-saw").shouldBeNull()

        wallet.moves shouldBe emptyList()
        wallet.reads shouldBe emptyList()
        dbRead { Spin.count() } shouldBe 0
    }

    test("a free round's bet costs nothing; its win pays real when the engine owns payouts") {
        service().place(leg("fs-bet", freespinId = "grant-1")) shouldBe Balance(1000, 500, "UAH")
        wallet.moves shouldBe emptyList()
        dbRead { Spin.findByExternalId("fs-bet")!!.let { it.amount to it.realAmount } } shouldBe (100L to 0L)
        events.last()["freespinId"]!!.jsonPrimitive.content shouldBe "grant-1"

        service(freespinToPayout = true).settle(leg("fs-win", amount = 300, freespinId = "grant-1")) shouldBe Balance(1300, 500, "UAH")
        wallet.moves.single().reference shouldBe "spin:settle:fs-win"
    }

    test("with payouts owned elsewhere no leg of a free round touches the wallet") {
        service(freespinToPayout = false).settle(leg("fs-win", amount = 300, freespinId = "grant-1")) shouldBe Balance(1000, 500, "UAH")

        wallet.moves shouldBe emptyList()
        // The row still carries the full amount with a zero split: "no money moved here".
        dbRead { Spin.findByExternalId("fs-win")!!.let { Triple(it.amount, it.realAmount, it.bonusAmount) } } shouldBe Triple(300L, 0L, 0L)
        events.events.size shouldBe 1
    }

    test("a wallet that fails leaves the leg unwritten, unpublished and the call failed") {
        wallet.failNextMove = IllegalStateException("pam is down")

        shouldThrow<IllegalStateException> { service().place(leg("leg-1")) }

        dbRead { Spin.count() } shouldBe 0
        events.events shouldBe emptyList()
    }

    test("a broker failure after the wallet moved is logged, never surfaced") {
        events.failNext = IllegalStateException("rabbit is down")

        service().place(leg("leg-1")) shouldBe Balance(900, 500, "UAH")
        dbRead { Spin.count() } shouldBe 1
    }

    test("closing publishes the round finished once; a late leg reopens it silently") {
        service().place(leg("leg-1"))
        service().closeRound("round-1")

        dbRead { CasinoRound.findByExternalId("round-1")!!.isFinished } shouldBe true
        val closed = events.only(RoundEvent::class.java).data().jsonObject
        closed["finishedAt"]!!.jsonPrimitive.content.isNotBlank() shouldBe true
        closed["externalId"]!!.jsonPrimitive.content shouldBe "round-1"
        closed["session"]!!.jsonObject["playerId"]!!.jsonPrimitive.content shouldBe "1"

        shouldThrow<CasinoRoundAlreadyFinishedException> { service().closeRound("round-1") }

        service().settle(leg("late-win", amount = 10))
        dbRead { CasinoRound.findByExternalId("round-1")!!.isFinished } shouldBe false
        events.events.filterIsInstance<RoundEvent>().size shouldBe 1
    }

    test("an unknown game does not refuse money: the round opens without one, like a sportsbook leg") {
        service().place(leg("bet-1", roundId = "sport-1", game = "no_such_game"))
        service().place(leg("bet-2", roundId = "sport-2", game = null))

        dbRead { CasinoRounds.selectAll().count() } shouldBe 2
        dbRead { CasinoRound.findByExternalId("sport-1")!!.game }.shouldBeNull()
        val data = events.last()
        data.containsKey("gameIdentity") shouldBe false
        data["round"]!!.jsonObject.containsKey("gameVariant") shouldBe false
    }

    test("two legs of one round share the row, and the leg id is what makes a redelivery collide") {
        service().place(leg("a", roundId = "r"))
        service().settle(leg("b", roundId = "r"))

        dbRead { Spins.selectAll().count() } shouldBe 2
        dbRead { CasinoRounds.selectAll().count() } shouldBe 1
    }
})
