package infrastructure.aggregator

import application.port.external.IFreespinPort
import application.port.external.IGamePort
import application.port.factory.AggregatorAdapterProvider
import domain.exception.badrequest.AggregatorNotSupportedException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import support.TestFixtures

class AggregatorRegistryTest : FunSpec({

    class FakeProvider(override val integration: String) : AggregatorAdapterProvider {
        val gamePort: IGamePort = mockk(relaxed = true)
        val freespinPort: IFreespinPort = mockk(relaxed = true)

        override fun createGameAdapter(config: Map<String, Any>): IGamePort = gamePort
        override fun createFreespinAdapter(config: Map<String, Any>): IFreespinPort = freespinPort
    }

    test("resolves game adapter by integration key") {
        val oneGameHub = FakeProvider("ONEGAMEHUB")
        val pragmatic = FakeProvider("PRAGMATIC")
        val registry = AggregatorRegistry(listOf(oneGameHub, pragmatic))

        val aggregator = TestFixtures.aggregator(integration = "PRAGMATIC")
        registry.createGameAdapter(aggregator) shouldBe pragmatic.gamePort
    }

    test("resolves freespin adapter by integration key") {
        val pateplay = FakeProvider("PATEPLAY")
        val registry = AggregatorRegistry(listOf(pateplay))

        val aggregator = TestFixtures.aggregator(integration = "PATEPLAY")
        registry.createFreespinAdapter(aggregator) shouldBe pateplay.freespinPort
    }

    test("unknown integration raises AggregatorNotSupportedException") {
        val registry = AggregatorRegistry(listOf(FakeProvider("ONEGAMEHUB")))
        val aggregator = TestFixtures.aggregator(integration = "UNKNOWN")
        shouldThrow<AggregatorNotSupportedException> {
            registry.createGameAdapter(aggregator)
        }
    }

    test("empty provider list — all integrations unsupported") {
        val registry = AggregatorRegistry(emptyList())
        val aggregator = TestFixtures.aggregator(integration = "ONEGAMEHUB")
        shouldThrow<AggregatorNotSupportedException> {
            registry.createFreespinAdapter(aggregator)
        }
    }
})
