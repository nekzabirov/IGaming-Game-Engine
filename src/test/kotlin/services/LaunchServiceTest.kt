package services

import db.Platform
import errors.CasinoGameNotActiveException
import errors.CasinoGameNotFoundException
import errors.CasinoProviderNotActiveException
import errors.UnsupportedPlatformException
import events.SessionEvent
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import support.FakeGameHub
import support.FakeLimits
import support.Fixtures
import support.RecordingEvents
import support.TestDatabase

class LaunchServiceTest : FunSpec({

    TestDatabase.connect()

    lateinit var hub: FakeGameHub
    lateinit var limits: FakeLimits
    lateinit var events: RecordingEvents

    fun service() = LaunchService(hub, limits, events)

    beforeTest {
        TestDatabase.reset()
        hub = FakeGameHub()
        limits = FakeLimits()
        events = RecordingEvents()
        val pragmatic = Fixtures.provider("pragmatic")
        val dormant = Fixtures.provider("dormant", active = false)
        Fixtures.game("gates_of_olympus", pragmatic)
        Fixtures.game("paused", pragmatic, active = false)
        Fixtures.game("orphan", dormant)
    }

    test("a launch goes through the hub, saves the bet cap first and publishes the session") {
        val url = service().play("gates_of_olympus", "1", "uk", Platform.MOBILE, "UAH", maxSpinPlaceAmount = 5_000)

        url shouldBe "https://vendor/launch/gates_of_olympus?player=1"
        limits.store["1"] shouldBe 5_000
        // An unsupported locale falls back to English instead of refusing the launch.
        hub.launches.single() shouldBe FakeGameHub.Launch("gates_of_olympus", "1", "UAH", "en", Platform.MOBILE, null)

        val session = events.only(SessionEvent::class.java).data().jsonObject
        session["playerId"]!!.jsonPrimitive.content shouldBe "1"
        session["gameIdentity"]!!.jsonPrimitive.content shouldBe "gates_of_olympus"
        session["gameProvider"]!!.jsonPrimitive.content shouldBe "pragmatic"
        session["platform"]!!.jsonPrimitive.content shouldBe "MOBILE"
        session["locale"]!!.jsonPrimitive.content shouldBe "en"
    }

    test("inactive game, inactive provider and unsupported platform are refused before the hub is called") {
        shouldThrow<CasinoGameNotActiveException> { service().play("paused", "1", "en", Platform.DESKTOP, "UAH", null) }
        shouldThrow<CasinoProviderNotActiveException> { service().play("orphan", "1", "en", Platform.DESKTOP, "UAH", null) }
        shouldThrow<UnsupportedPlatformException> { service().play("gates_of_olympus", "1", "en", Platform.DOWNLOAD, "UAH", null) }
        shouldThrow<CasinoGameNotFoundException> { service().play("nope", "1", "en", Platform.DESKTOP, "UAH", null) }

        hub.launches shouldBe emptyList()
        events.events shouldBe emptyList()
    }

    test("a hub refusal publishes nothing") {
        hub.launchFailure = IllegalStateException("vendor down")

        shouldThrow<IllegalStateException> { service().play("gates_of_olympus", "1", "en", Platform.DESKTOP, "UAH", null) }
        events.events shouldBe emptyList()
    }

    test("demo needs the game to exist but not to be active; presets flatten the hub's bounds") {
        service().demo("paused", "UAH", "en", Platform.DESKTOP, "https://lobby") shouldBe "https://vendor/demo/paused"
        shouldThrow<CasinoGameNotFoundException> { service().demo("nope", "UAH", "en", Platform.DESKTOP, "") }

        service().freespinPresets("gates_of_olympus") shouldBe mapOf("paylines" to "20", "minAmount" to 1000L)
    }
})
