package events

import com.rabbitmq.client.AlreadyClosedException
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.MessageProperties
import com.rabbitmq.client.ShutdownSignalException
import db.Platform
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.datetime.Instant
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.IOException

class RabbitEventPublisherTest : FunSpec({

    val round = RoundPayload(
        id = 7,
        externalId = "r-1",
        freespinId = null,
        playerId = "42",
        game = null,
        currency = "UAH",
        createdAt = Instant.parse("2026-09-06T10:00:00Z"),
        finishedAt = null,
    )

    fun openChannel(): Channel = mockk<Channel>(relaxed = true).also { every { it.isOpen } returns true }

    test("publishes a persistent envelope on the event's route and waits for the confirm") {
        val channel = openChannel()
        val connection = mockk<Connection> { every { createChannel() } returns channel }
        val body = slot<ByteArray>()

        RabbitEventPublisher(connection, "casino.events").publish(RoundEvent(round))

        verify(exactly = 1) { channel.confirmSelect() }
        verify(exactly = 1) { channel.basicPublish("casino.events", "round.events", MessageProperties.PERSISTENT_BASIC, capture(body)) }
        verify(exactly = 1) { channel.waitForConfirmsOrDie(5_000L) }

        val envelope = appJson.parseToJsonElement(body.captured.decodeToString()).jsonObject
        envelope["playerId"]!!.jsonPrimitive.content shouldBe "42"
        val data = envelope["data"]!!.jsonObject
        data["externalId"]!!.jsonPrimitive.content shouldBe "r-1"
        data["currency"]!!.jsonPrimitive.content shouldBe "UAH"
        // The legacy nested shape rides along, and explicit nulls stay on the wire.
        data["session"]!!.jsonObject["playerId"]!!.jsonPrimitive.content shouldBe "42"
        data.containsKey("game") shouldBe true
        data.containsKey("gameVariant") shouldBe false
    }

    test("a channel closed mid-publish is re-created and the batch retried once") {
        val dead = openChannel().also {
            every { it.basicPublish(any(), any(), any(), any()) } throws AlreadyClosedException(ShutdownSignalException(false, false, null, null))
        }
        val alive = openChannel()
        val connection = mockk<Connection> { every { createChannel() } returnsMany listOf(dead, alive) }

        RabbitEventPublisher(connection, "casino.events").publish(RoundEvent(round))

        verify(exactly = 1) { alive.basicPublish(any(), eq("round.events"), any(), any()) }
        verify(exactly = 1) { alive.waitForConfirmsOrDie(any<Long>()) }
    }

    test("a broker that does not confirm fails the publish and drops the channel") {
        val first = openChannel().also { every { it.waitForConfirmsOrDie(any<Long>()) } throws IOException("nack") }
        val second = openChannel()
        val connection = mockk<Connection> { every { createChannel() } returnsMany listOf(first, second) }
        val publisher = RabbitEventPublisher(connection, "casino.events")

        shouldThrow<IOException> { publisher.publish(RoundEvent(round)) }

        // The next publish opens a fresh channel instead of reusing the one the broker closed.
        publisher.publish(SessionEvent(SessionPayload("42", game(), "UAH", "en", Platform.DESKTOP, Instant.parse("2026-09-06T10:00:00Z"))))
        verify(exactly = 1) { second.basicPublish(any(), eq("session.events"), any(), any()) }
    }
})

private fun game() = GamePayload(
    identity = "gates_of_olympus",
    name = "Gates of Olympus",
    provider = ProviderPayload("pragmatic", "Pragmatic", emptyMap(), emptyMap(), 1, true, emptyList(), emptyList(), emptyList()),
    collections = emptyList(),
    bonusBetEnable = true,
    bonusWageringEnable = true,
    tags = listOf("slots"),
    rtp = null,
    freeSpinEnable = true,
    freeChipEnable = false,
    jackpotEnable = false,
    demoEnable = true,
    bonusBuyEnable = false,
    locales = listOf("en"),
    platforms = listOf(Platform.DESKTOP),
    playLines = 20,
    active = true,
    images = emptyMap(),
    customImages = emptyMap(),
    customTags = emptyList(),
    order = 0,
)
