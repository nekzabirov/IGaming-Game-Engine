package services

import com.nekgamebling.game.v1.CasinoGameFilter
import db.CasinoGameCollections
import db.CasinoGames
import db.Pageable
import errors.CasinoGameNotFoundException
import errors.CollectionNotFoundException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.sql.selectAll
import plugins.dbRead
import support.Fixtures
import support.TestDatabase

class CollectionServiceTest : FunSpec({

    TestDatabase.connect()

    val service = CollectionService()
    val page = Pageable(1, 20)

    beforeTest {
        TestDatabase.reset()
        val provider = Fixtures.provider("pragmatic")
        Fixtures.game("g1", provider, name = "Gates of Olympus")
        Fixtures.game("g2", provider, name = "Sweet Bonanza")
        Fixtures.game("g3", provider, name = "Starburst")
        Fixtures.collection("popular")
    }

    test("members are appended at the end and adding twice is a no-op") {
        service.addGame("popular", "g2")
        service.addGame("popular", "g1")
        service.addGame("popular", "g2")

        dbRead { CasinoGameCollections.selectAll().count() } shouldBe 2
        service.games("popular", CasinoGameFilter.getDefaultInstance(), page).itemsList.map { it.identity } shouldBe listOf("g2", "g1")
    }

    test("order can be changed for a member only, and removal is idempotent") {
        service.addGame("popular", "g1")
        service.addGame("popular", "g2")

        service.updateGameOrder("popular", "g2", -1)
        service.games("popular", CasinoGameFilter.getDefaultInstance(), page).itemsList.map { it.identity } shouldBe listOf("g2", "g1")

        shouldThrow<CasinoGameNotFoundException> { service.updateGameOrder("popular", "g3", 0) }
        shouldThrow<CollectionNotFoundException> { service.addGame("ghost", "g1") }

        service.removeGame("popular", "g1")
        service.removeGame("popular", "g1")
        service.games("popular", CasinoGameFilter.getDefaultInstance(), page).itemsList.map { it.identity } shouldBe listOf("g2")
    }

    test("the rail is filtered like any listing and reports its providers") {
        service.addGame("popular", "g1")
        service.addGame("popular", "g3")

        val filtered = service.games("popular", CasinoGameFilter.newBuilder().setQuery("starbrust").build(), page)
        filtered.itemsList.map { it.identity } shouldBe listOf("g3")
        filtered.providersList.map { it.identity } shouldBe listOf("pragmatic")
        filtered.totalItems shouldBe 1
    }

    test("deleting a rail drops its memberships and keeps the games") {
        service.addGame("popular", "g1")

        service.delete("popular")

        shouldThrow<CollectionNotFoundException> { service.find("popular") }
        dbRead { CasinoGameCollections.selectAll().count() } shouldBe 0
        dbRead { CasinoGames.selectAll().count() } shouldBe 3
        shouldThrow<CollectionNotFoundException> { service.delete("popular") }
    }

    test("save creates or updates without touching images; updateImage writes one key") {
        service.save("new_rail", mapOf("en" to "New"), listOf("home"), active = true, order = 3)
        service.updateImage("new_rail", "cover", "https://cdn/cover.webp")
        service.save("new_rail", mapOf("en" to "Newer"), emptyList(), active = false, order = 4)

        val dto = service.find("new_rail")
        dto.nameMap shouldBe mapOf("en" to "Newer")
        dto.imagesMap shouldBe mapOf("cover" to "https://cdn/cover.webp")
        dto.active shouldBe false
        dto.order shouldBe 4

        service.findAll(query = "", active = null, inTags = emptyList(), inProviders = listOf("pragmatic"), pageable = page).items shouldBe emptyList()
        service.addGame("new_rail", "g1")
        service.findAll(query = "", active = null, inTags = emptyList(), inProviders = listOf("pragmatic"), pageable = page).items.map { it.identity } shouldBe listOf("new_rail")
    }
})
