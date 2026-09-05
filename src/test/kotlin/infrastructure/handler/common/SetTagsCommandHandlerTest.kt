package infrastructure.handler.common

import application.command.game.SetCasinoGameTagsCommand
import application.command.provider.SetCasinoProviderTagsCommand
import domain.model.CasinoGame
import domain.model.CasinoProvider
import domain.repository.ICasinoGameRepository
import domain.repository.ICasinoProviderRepository
import domain.vo.Identity
import domain.vo.Page
import domain.vo.Pageable
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * The point of these cases is WHICH column the write lands in: `custom_tags`, never `tags`.
 * The synced list is the hub's and the catalog sync rewrites it wholesale on every run, so a tag
 * put there would vanish on the next pass — which is exactly the bug this split exists to prevent.
 * Fakes rather than mockk for the same reason as [SetImageCommandHandlerTest].
 */
class SetTagsCommandHandlerTest : FunSpec({

    class FakeCasinoGameRepo : ICasinoGameRepository {
        val customTagCalls = mutableListOf<Pair<Identity, List<String>>>()
        val saved = mutableListOf<CasinoGame>()
        override suspend fun save(game: CasinoGame): CasinoGame = game.also { saved += it }
        override suspend fun saveAll(gameList: List<CasinoGame>): List<CasinoGame> = gameList
        override suspend fun findByIdentity(identity: Identity): CasinoGame? = null
        override suspend fun findAll(pageable: Pageable): Page<CasinoGame> = Page(emptyList(), 0, 0, 0)
        override suspend fun findAll(): List<CasinoGame> = emptyList()
        override suspend fun addImage(identity: Identity, key: String, url: String) = Unit
        override suspend fun setCustomTags(identity: Identity, tags: List<String>) {
            customTagCalls += identity to tags
        }
    }

    class FakeCasinoProviderRepo : ICasinoProviderRepository {
        val customTagCalls = mutableListOf<Pair<Identity, List<String>>>()
        override suspend fun save(provider: CasinoProvider): CasinoProvider = provider
        override suspend fun saveAll(providers: List<CasinoProvider>): List<CasinoProvider> = providers
        override suspend fun findByIdentity(identity: Identity): CasinoProvider? = null
        override suspend fun findAll(pageable: Pageable): Page<CasinoProvider> = Page(emptyList(), 0, 0, 0)
        override suspend fun findAll(): List<CasinoProvider> = emptyList()
        override suspend fun addImage(identity: Identity, key: String, url: String) = Unit
        override suspend fun setCustomTags(identity: Identity, tags: List<String>) {
            customTagCalls += identity to tags
        }
    }

    test("game tags land in custom_tags, and no game row is re-saved through the synced path") {
        val gameRepo = FakeCasinoGameRepo()
        val providerRepo = FakeCasinoProviderRepo()

        val result = SetTagsCommandHandler(gameRepo, providerRepo)
            .handle(SetCasinoGameTagsCommand(Identity("game_a"), listOf("prematch_chose", "new")))

        result.isSuccess shouldBe true
        gameRepo.customTagCalls.single() shouldBe (Identity("game_a") to listOf("prematch_chose", "new"))
        // save() would carry the hub's `tags` back to the DB — the local write must not go through it
        gameRepo.saved.size shouldBe 0
        providerRepo.customTagCalls.size shouldBe 0
    }

    test("provider tags land in the provider repository") {
        val gameRepo = FakeCasinoGameRepo()
        val providerRepo = FakeCasinoProviderRepo()

        val result = SetTagsCommandHandler(gameRepo, providerRepo)
            .handle(SetCasinoProviderTagsCommand(Identity("prov_a"), listOf("live")))

        result.isSuccess shouldBe true
        providerRepo.customTagCalls.single() shouldBe (Identity("prov_a") to listOf("live"))
        gameRepo.customTagCalls.size shouldBe 0
    }

    test("resolvedTags shows the hub's tags and ours as one deduplicated list") {
        val provider = CasinoProvider(identity = Identity("prov_a"), name = "Prov")
        val game = CasinoGame(
            identity = Identity("game_a"),
            name = "Game",
            provider = provider,
            tags = listOf("slot", "new"),
            customTags = listOf("prematch_chose", "new"),
        )

        game.resolvedTags() shouldBe listOf("slot", "new", "prematch_chose")
    }
})
