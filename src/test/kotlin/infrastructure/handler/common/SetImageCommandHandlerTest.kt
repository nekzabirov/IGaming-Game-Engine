package infrastructure.handler.common

import application.command.collection.SetCollectionImageCommand
import application.command.game.SetCasinoGameImageCommand
import application.command.provider.SetCasinoProviderImageCommand
import domain.exception.badrequest.BlankImageUrlException
import domain.model.Collection
import domain.model.CasinoGame
import domain.model.CasinoProvider
import domain.repository.ICollectionRepository
import domain.repository.ICasinoGameRepository
import domain.repository.ICasinoProviderRepository
import domain.vo.Identity
import domain.vo.Page
import domain.vo.Pageable
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Verifies the polymorphic [SetImageCommandHandler] dispatches each sealed sub-command
 * to the correct repository. Uses hand-rolled fake repositories instead of mockk because
 * mockk's argument matchers trip over `@JvmInline value class Identity` (signature
 * generator instantiates the value class with a bogus default, triggering the init
 * validation).
 */
class SetImageCommandHandlerTest : FunSpec({

    val sampleUrl = "https://cdn.example.com/casino/game/game_a/main.webp"

    class FakeCasinoGameRepo : ICasinoGameRepository {
        val calls = mutableListOf<Triple<Identity, String, String>>()
        override suspend fun save(game: CasinoGame): CasinoGame = game
        override suspend fun saveAll(gameList: List<CasinoGame>): List<CasinoGame> = gameList
        override suspend fun findByIdentity(identity: Identity): CasinoGame? = null
        override suspend fun findAll(pageable: Pageable): Page<CasinoGame> = Page(emptyList(), 0, 0, 0)
        override suspend fun findAll(): List<CasinoGame> = emptyList()
        override suspend fun addImage(identity: Identity, key: String, url: String) {
            calls += Triple(identity, key, url)
        }
        override suspend fun setCustomTags(identity: Identity, tags: List<String>) = Unit
    }

    class FakeCasinoProviderRepo : ICasinoProviderRepository {
        val calls = mutableListOf<Triple<Identity, String, String>>()
        override suspend fun save(provider: CasinoProvider): CasinoProvider = provider
        override suspend fun saveAll(providers: List<CasinoProvider>): List<CasinoProvider> = providers
        override suspend fun findByIdentity(identity: Identity): CasinoProvider? = null
        override suspend fun findAll(pageable: Pageable): Page<CasinoProvider> = Page(emptyList(), 0, 0, 0)
        override suspend fun findAll(): List<CasinoProvider> = emptyList()
        override suspend fun addImage(identity: Identity, key: String, url: String) {
            calls += Triple(identity, key, url)
        }
        override suspend fun setCustomTags(identity: Identity, tags: List<String>) = Unit
    }

    class FakeCollectionRepo : ICollectionRepository {
        val calls = mutableListOf<Triple<Identity, String, String>>()
        override suspend fun save(collection: Collection): Collection = collection
        override suspend fun findByIdentity(identity: Identity): Collection? = null
        override suspend fun findAll(pageable: Pageable): Page<Collection> = Page(emptyList(), 0, 0, 0)
        override suspend fun addImage(identity: Identity, key: String, url: String) {
            calls += Triple(identity, key, url)
        }
        override suspend fun addCasinoGame(identity: Identity, gameIdentity: Identity) = Unit
        override suspend fun removeCasinoGame(identity: Identity, gameIdentity: Identity) = Unit
        override suspend fun updateCasinoGameOrder(identity: Identity, gameIdentity: Identity, order: Int) = Unit
        override suspend fun deleteByIdentity(identity: Identity) = Unit
    }

    fun handler(
        gameRepo: FakeCasinoGameRepo = FakeCasinoGameRepo(),
        providerRepo: FakeCasinoProviderRepo = FakeCasinoProviderRepo(),
        collectionRepo: FakeCollectionRepo = FakeCollectionRepo(),
    ) = SetImageCommandHandler(
        gameRepository = gameRepo,
        providerRepository = providerRepo,
        collectionRepository = collectionRepo,
    )

    test("SetCasinoGameImageCommand stores the URL via the game repository") {
        val gameRepo = FakeCasinoGameRepo()
        val providerRepo = FakeCasinoProviderRepo()
        val collectionRepo = FakeCollectionRepo()

        val result = handler(gameRepo, providerRepo, collectionRepo)
            .handle(SetCasinoGameImageCommand(Identity("game_a"), "main", sampleUrl))

        result.isSuccess shouldBe true
        gameRepo.calls.single() shouldBe Triple(Identity("game_a"), "main", sampleUrl)
        providerRepo.calls.size shouldBe 0
        collectionRepo.calls.size shouldBe 0
    }

    test("SetCasinoProviderImageCommand stores the URL via the provider repository") {
        val gameRepo = FakeCasinoGameRepo()
        val providerRepo = FakeCasinoProviderRepo()
        val collectionRepo = FakeCollectionRepo()

        val result = handler(gameRepo, providerRepo, collectionRepo)
            .handle(SetCasinoProviderImageCommand(Identity("prov_a"), "logo", sampleUrl))

        result.isSuccess shouldBe true
        providerRepo.calls.single() shouldBe Triple(Identity("prov_a"), "logo", sampleUrl)
        gameRepo.calls.size shouldBe 0
        collectionRepo.calls.size shouldBe 0
    }

    test("SetCollectionImageCommand stores the URL via the collection repository") {
        val gameRepo = FakeCasinoGameRepo()
        val providerRepo = FakeCasinoProviderRepo()
        val collectionRepo = FakeCollectionRepo()

        val result = handler(gameRepo, providerRepo, collectionRepo)
            .handle(SetCollectionImageCommand(Identity("coll_a"), "cover", sampleUrl))

        result.isSuccess shouldBe true
        collectionRepo.calls.single() shouldBe Triple(Identity("coll_a"), "cover", sampleUrl)
        gameRepo.calls.size shouldBe 0
        providerRepo.calls.size shouldBe 0
    }

    test("blank URL is rejected with BlankImageUrlException and nothing is stored") {
        val gameRepo = FakeCasinoGameRepo()

        val result = handler(gameRepo = gameRepo)
            .handle(SetCasinoGameImageCommand(Identity("game_a"), "main", "  "))

        result.isFailure shouldBe true
        shouldThrow<BlankImageUrlException> { result.getOrThrow() }
        gameRepo.calls.size shouldBe 0
    }
})
