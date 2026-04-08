package infrastructure.handler.collection

import application.command.collection.AddCollectionGameCommand
import application.command.collection.RemoveCollectionGameCommand
import application.command.collection.UpdateCollectionGameOrderCommand
import domain.exception.notfound.CollectionNotFoundException
import domain.exception.notfound.GameNotFoundException
import domain.model.Collection
import domain.repository.ICollectionRepository
import domain.vo.Identity
import domain.vo.Page
import domain.vo.Pageable
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Fake-based tests for the three single-game collection-membership handlers.
 * Mirrors the pattern in `SetImageCommandHandlerTest` — no mockk because
 * value-class parameters trip mockk's signature generator.
 */
class CollectionGameMembershipHandlerTest : FunSpec({

    class FakeCollectionRepo(
        private val knownCollections: Set<String>,
        private val knownGames: Set<String>,
        private val membership: MutableSet<Pair<String, String>> = mutableSetOf(),
    ) : ICollectionRepository {
        val addCalls = mutableListOf<Pair<Identity, Identity>>()
        val removeCalls = mutableListOf<Pair<Identity, Identity>>()
        val orderCalls = mutableListOf<Triple<Identity, Identity, Int>>()

        override suspend fun save(collection: Collection): Collection = collection
        override suspend fun findByIdentity(identity: Identity): Collection? = null
        override suspend fun findAll(pageable: Pageable): Page<Collection> = Page(emptyList(), 0, 0, 0)
        override suspend fun addImage(identity: Identity, key: String, url: String) = Unit

        override suspend fun addGame(identity: Identity, gameIdentity: Identity) {
            if (identity.value !in knownCollections) throw CollectionNotFoundException()
            if (gameIdentity.value !in knownGames) throw GameNotFoundException()
            addCalls += identity to gameIdentity
            membership += identity.value to gameIdentity.value
        }

        override suspend fun removeGame(identity: Identity, gameIdentity: Identity) {
            if (identity.value !in knownCollections) throw CollectionNotFoundException()
            if (gameIdentity.value !in knownGames) throw GameNotFoundException()
            removeCalls += identity to gameIdentity
            membership -= identity.value to gameIdentity.value
        }

        override suspend fun updateGameOrder(identity: Identity, gameIdentity: Identity, order: Int) {
            if (identity.value !in knownCollections) throw CollectionNotFoundException()
            if (gameIdentity.value !in knownGames) throw GameNotFoundException()
            // Simulate "game must be a member of this collection" check.
            if ((identity.value to gameIdentity.value) !in membership) throw GameNotFoundException()
            orderCalls += Triple(identity, gameIdentity, order)
        }
    }

    // ---------------------------------------------------------------------
    // AddCollectionGameCommandHandler
    // ---------------------------------------------------------------------

    test("AddCollectionGame — happy path forwards identities to repository") {
        val repo = FakeCollectionRepo(
            knownCollections = setOf("popular"),
            knownGames = setOf("g1"),
        )
        val handler = AddCollectionGameCommandHandler(repo)

        val result = handler.handle(
            AddCollectionGameCommand(
                identity = Identity("popular"),
                gameIdentity = Identity("g1"),
            )
        )

        result.isSuccess shouldBe true
        repo.addCalls.single() shouldBe (Identity("popular") to Identity("g1"))
    }

    test("AddCollectionGame — missing collection raises CollectionNotFoundException") {
        val repo = FakeCollectionRepo(
            knownCollections = emptySet(),
            knownGames = setOf("g1"),
        )
        val handler = AddCollectionGameCommandHandler(repo)

        val result = handler.handle(
            AddCollectionGameCommand(Identity("ghost"), Identity("g1"))
        )

        result.isFailure shouldBe true
        result.exceptionOrNull().shouldBeInstanceOf<CollectionNotFoundException>()
        repo.addCalls.size shouldBe 0
    }

    test("AddCollectionGame — missing game raises GameNotFoundException") {
        val repo = FakeCollectionRepo(
            knownCollections = setOf("popular"),
            knownGames = emptySet(),
        )
        val handler = AddCollectionGameCommandHandler(repo)

        val result = handler.handle(
            AddCollectionGameCommand(Identity("popular"), Identity("missing"))
        )

        result.isFailure shouldBe true
        result.exceptionOrNull().shouldBeInstanceOf<GameNotFoundException>()
    }

    // ---------------------------------------------------------------------
    // RemoveCollectionGameCommandHandler
    // ---------------------------------------------------------------------

    test("RemoveCollectionGame — happy path forwards identities to repository") {
        val repo = FakeCollectionRepo(
            knownCollections = setOf("popular"),
            knownGames = setOf("g1"),
            membership = mutableSetOf("popular" to "g1"),
        )
        val handler = RemoveCollectionGameCommandHandler(repo)

        val result = handler.handle(
            RemoveCollectionGameCommand(Identity("popular"), Identity("g1"))
        )

        result.isSuccess shouldBe true
        repo.removeCalls.single() shouldBe (Identity("popular") to Identity("g1"))
    }

    test("RemoveCollectionGame — missing collection raises CollectionNotFoundException") {
        val repo = FakeCollectionRepo(
            knownCollections = emptySet(),
            knownGames = setOf("g1"),
        )
        val handler = RemoveCollectionGameCommandHandler(repo)

        val result = handler.handle(
            RemoveCollectionGameCommand(Identity("ghost"), Identity("g1"))
        )

        result.isFailure shouldBe true
        result.exceptionOrNull().shouldBeInstanceOf<CollectionNotFoundException>()
    }

    // ---------------------------------------------------------------------
    // UpdateCollectionGameOrderCommandHandler
    // ---------------------------------------------------------------------

    test("UpdateCollectionGameOrder — happy path forwards order to repository") {
        val repo = FakeCollectionRepo(
            knownCollections = setOf("popular"),
            knownGames = setOf("g1"),
            membership = mutableSetOf("popular" to "g1"),
        )
        val handler = UpdateCollectionGameOrderCommandHandler(repo)

        val result = handler.handle(
            UpdateCollectionGameOrderCommand(
                identity = Identity("popular"),
                gameIdentity = Identity("g1"),
                order = 7,
            )
        )

        result.isSuccess shouldBe true
        repo.orderCalls.single() shouldBe Triple(Identity("popular"), Identity("g1"), 7)
    }

    test("UpdateCollectionGameOrder — game not in collection raises GameNotFoundException") {
        val repo = FakeCollectionRepo(
            knownCollections = setOf("popular"),
            knownGames = setOf("g1"),
            membership = mutableSetOf(), // known game but NOT in this collection
        )
        val handler = UpdateCollectionGameOrderCommandHandler(repo)

        val result = handler.handle(
            UpdateCollectionGameOrderCommand(Identity("popular"), Identity("g1"), 0)
        )

        result.isFailure shouldBe true
        result.exceptionOrNull().shouldBeInstanceOf<GameNotFoundException>()
    }
})
