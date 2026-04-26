package infrastructure.handler.common

import application.command.collection.SetCollectionImageCommand
import application.command.game.SetGameImageCommand
import application.command.provider.SetProviderImageCommand
import domain.repository.ICollectionRepository
import domain.repository.IGameRepository
import domain.repository.IGameVariantRepository
import domain.repository.IProviderRepository
import application.port.external.FileAdapter
import application.port.external.MediaFile
import domain.model.Collection
import domain.model.Game
import domain.model.GameVariant
import domain.model.Provider
import domain.vo.FileUpload
import domain.vo.Identity
import domain.vo.Page
import domain.vo.Pageable
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

    val sampleFile = FileUpload(name = "banner.png", content = byteArrayOf(1, 2, 3))

    class FakeGameRepo : IGameRepository {
        val calls = mutableListOf<Triple<Identity, String, String>>()
        override suspend fun save(game: Game): Game = game
        override suspend fun saveAll(gameList: List<Game>): List<Game> = gameList
        override suspend fun findByIdentity(identity: Identity): Game? = null
        override suspend fun findAll(pageable: Pageable): Page<Game> = Page(emptyList(), 0, 0, 0)
        override suspend fun findAll(): List<Game> = emptyList()
        override suspend fun addImage(identity: Identity, key: String, url: String) {
            calls += Triple(identity, key, url)
        }
    }

    class FakeProviderRepo : IProviderRepository {
        val calls = mutableListOf<Triple<Identity, String, String>>()
        override suspend fun save(provider: Provider): Provider = provider
        override suspend fun saveAll(providers: List<Provider>): List<Provider> = providers
        override suspend fun findByIdentity(identity: Identity): Provider? = null
        override suspend fun findAll(pageable: Pageable): Page<Provider> = Page(emptyList(), 0, 0, 0)
        override suspend fun findAll(): List<Provider> = emptyList()
        override suspend fun addImage(identity: Identity, key: String, url: String) {
            calls += Triple(identity, key, url)
        }
    }

    class FakeCollectionRepo : ICollectionRepository {
        val calls = mutableListOf<Triple<Identity, String, String>>()
        override suspend fun save(collection: Collection): Collection = collection
        override suspend fun findByIdentity(identity: Identity): Collection? = null
        override suspend fun findAll(pageable: Pageable): Page<Collection> = Page(emptyList(), 0, 0, 0)
        override suspend fun addImage(identity: Identity, key: String, url: String) {
            calls += Triple(identity, key, url)
        }
        override suspend fun addGame(identity: Identity, gameIdentity: Identity) = Unit
        override suspend fun removeGame(identity: Identity, gameIdentity: Identity) = Unit
        override suspend fun updateGameOrder(identity: Identity, gameIdentity: Identity, order: Int) = Unit
    }

    class FakeFileAdapter : FileAdapter {
        var lastFolder: String = ""
        override suspend fun upload(folder: String, fileName: String, file: MediaFile): Result<String> {
            lastFolder = folder
            return Result.success("https://cdn/$folder/$fileName")
        }
        override suspend fun delete(path: String): Result<Boolean> = Result.success(true)
    }

    test("SetGameImageCommand uploads with folder=casino/game and hits game repository") {
        val gameRepo = FakeGameRepo()
        val providerRepo = FakeProviderRepo()
        val collectionRepo = FakeCollectionRepo()
        val fileAdapter = FakeFileAdapter()
        val handler = SetImageCommandHandler(
            fileAdapter = fileAdapter,
            gameRepository = gameRepo,
            providerRepository = providerRepo,
            collectionRepository = collectionRepo,
        )

        val result = handler.handle(SetGameImageCommand(Identity("game_a"), "main", sampleFile))

        result.isSuccess shouldBe true
        fileAdapter.lastFolder shouldBe "casino/game"
        gameRepo.calls.size shouldBe 1
        gameRepo.calls.single() shouldBe Triple(Identity("game_a"), "main", "https://cdn/casino/game/game_a/main")
        providerRepo.calls.size shouldBe 0
        collectionRepo.calls.size shouldBe 0
    }

    test("SetProviderImageCommand uploads with folder=casino/provider and hits provider repository") {
        val gameRepo = FakeGameRepo()
        val providerRepo = FakeProviderRepo()
        val collectionRepo = FakeCollectionRepo()
        val fileAdapter = FakeFileAdapter()
        val handler = SetImageCommandHandler(
            fileAdapter = fileAdapter,
            gameRepository = gameRepo,
            providerRepository = providerRepo,
            collectionRepository = collectionRepo,
        )

        val result = handler.handle(SetProviderImageCommand(Identity("prov_a"), "logo", sampleFile))

        result.isSuccess shouldBe true
        fileAdapter.lastFolder shouldBe "casino/provider"
        providerRepo.calls.single() shouldBe Triple(Identity("prov_a"), "logo", "https://cdn/casino/provider/prov_a/logo")
        gameRepo.calls.size shouldBe 0
        collectionRepo.calls.size shouldBe 0
    }

    test("SetCollectionImageCommand uploads with folder=casino/collection and hits collection repository") {
        val gameRepo = FakeGameRepo()
        val providerRepo = FakeProviderRepo()
        val collectionRepo = FakeCollectionRepo()
        val fileAdapter = FakeFileAdapter()
        val handler = SetImageCommandHandler(
            fileAdapter = fileAdapter,
            gameRepository = gameRepo,
            providerRepository = providerRepo,
            collectionRepository = collectionRepo,
        )

        val result = handler.handle(SetCollectionImageCommand(Identity("coll_a"), "cover", sampleFile))

        result.isSuccess shouldBe true
        fileAdapter.lastFolder shouldBe "casino/collection"
        collectionRepo.calls.single() shouldBe Triple(Identity("coll_a"), "cover", "https://cdn/casino/collection/coll_a/cover")
        gameRepo.calls.size shouldBe 0
        providerRepo.calls.size shouldBe 0
    }
})
