package infrastructure.handler.aggregator

import application.command.aggregator.DeleteAggregatorCommand
import application.query.aggregator.BatchAggregatorQuery
import domain.exception.notfound.AggregatorNotFoundException
import domain.model.Aggregator
import domain.repository.IAggregatorRepository
import domain.vo.Identity
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class AggregatorHandlerTest : FunSpec({

    fun aggregatorOf(id: String) = Aggregator(
        identity = Identity(id),
        integration = "ONEGAMEHUB",
        config = emptyMap(),
        active = true,
    )

    class FakeAggregatorRepo(private val store: Map<String, Aggregator>) : IAggregatorRepository {
        val deleted = mutableListOf<Identity>()
        var lastBatchQuery: List<Identity> = emptyList()
        override suspend fun save(aggregator: Aggregator): Aggregator = aggregator
        override suspend fun findByIdentity(identity: Identity): Aggregator? = store[identity.value]
        override suspend fun findAllByIdentities(identities: List<Identity>): List<Aggregator> {
            lastBatchQuery = identities
            return identities.mapNotNull { store[it.value] }
        }
        override suspend fun findAll(): List<Aggregator> = store.values.toList()
        override suspend fun deleteByIdentity(identity: Identity) {
            if (identity.value !in store) throw AggregatorNotFoundException()
            deleted += identity
        }
    }

    test("DeleteAggregatorCommandHandler forwards identity on happy path") {
        val repo = FakeAggregatorRepo(store = mapOf("agg_a" to aggregatorOf("agg_a")))
        val handler = DeleteAggregatorCommandHandler(repo)

        val result = handler.handle(DeleteAggregatorCommand(Identity("agg_a")))

        result.isSuccess shouldBe true
        repo.deleted.single() shouldBe Identity("agg_a")
    }

    test("DeleteAggregatorCommandHandler returns failure when missing") {
        val repo = FakeAggregatorRepo(store = emptyMap())
        val handler = DeleteAggregatorCommandHandler(repo)

        val result = handler.handle(DeleteAggregatorCommand(Identity("missing")))

        result.isFailure shouldBe true
        result.exceptionOrNull().shouldBeInstanceOf<AggregatorNotFoundException>()
        repo.deleted.size shouldBe 0
    }

    test("BatchAggregatorQueryHandler delegates to repository.findAllByIdentities") {
        val repo = FakeAggregatorRepo(
            store = mapOf(
                "agg_a" to aggregatorOf("agg_a"),
                "agg_b" to aggregatorOf("agg_b"),
            ),
        )
        val handler = BatchAggregatorQueryHandler(repo)

        val result = handler.handle(
            BatchAggregatorQuery(
                identities = listOf(Identity("agg_a"), Identity("agg_b"), Identity("missing")),
            )
        )

        result.map { it.identity.value } shouldContainExactly listOf("agg_a", "agg_b")
        repo.lastBatchQuery.map { it.value } shouldContainExactly listOf("agg_a", "agg_b", "missing")
    }

    test("BatchAggregatorQueryHandler returns empty list when no matches") {
        val repo = FakeAggregatorRepo(store = emptyMap())
        val handler = BatchAggregatorQueryHandler(repo)

        val result = handler.handle(BatchAggregatorQuery(identities = listOf(Identity("agg_x"))))

        result.size shouldBe 0
    }
})
