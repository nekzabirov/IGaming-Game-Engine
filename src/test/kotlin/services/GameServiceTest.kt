package services

import com.nekgamebling.game.v1.CasinoGameFilter
import db.CasinoGame
import db.Pageable
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import plugins.dbRead
import plugins.dbTransaction
import support.Fixtures
import support.TestDatabase

class GameServiceTest : FunSpec({

    TestDatabase.connect()

    val service = GameService()
    val page = Pageable(1, 20)
    val noFilter: CasinoGameFilter = CasinoGameFilter.getDefaultInstance()

    beforeTest {
        TestDatabase.reset()
        val pragmatic = Fixtures.provider("pragmatic", tags = listOf("live"))
        val dormant = Fixtures.provider("dormant", active = false)
        Fixtures.game("gates_of_olympus", pragmatic, name = "Gates of Olympus", tags = listOf("slots"), rtp = 97.1, order = -1)
        Fixtures.game("sweet_bonanza", pragmatic, name = "Sweet Bonanza", tags = listOf("slots", "new"), rtp = 94.0, order = 5)
        Fixtures.game("book_of_ra", pragmatic, name = "Book of Ra", rtp = null, order = 2)
        Fixtures.game("hidden", dormant, name = "Hidden Game")
    }

    test("a listing hides games of inactive providers and follows the catalog order") {
        val result = service.findAll(noFilter, page)

        result.itemsList.map { it.identity } shouldBe listOf("gates_of_olympus", "book_of_ra", "sweet_bonanza")
        result.totalItems shouldBe 3
        result.providersList.map { it.identity } shouldBe listOf("pragmatic")
    }

    test("search survives typos, missing words and pasted-together words, and ranks the exact hit first") {
        service.findAll(CasinoGameFilter.newBuilder().setQuery("gates olimpus").build(), page).itemsList.map { it.identity } shouldBe listOf("gates_of_olympus")
        service.findAll(CasinoGameFilter.newBuilder().setQuery("bonanca").build(), page).itemsList.map { it.identity } shouldBe listOf("sweet_bonanza")
        service.findAll(CasinoGameFilter.newBuilder().setQuery("bookofra").build(), page).itemsList.map { it.identity } shouldBe listOf("book_of_ra")
        service.findAll(CasinoGameFilter.newBuilder().setQuery("zzzzzz").build(), page).itemsList shouldBe emptyList()
    }

    test("tags on the wire are the merged set; UpdateTags rewrites only the local half") {
        service.updateTags("sweet_bonanza", listOf(" prematch_chose ", "new", ""))

        val game = service.find("sweet_bonanza").item
        game.tagsList shouldBe listOf("slots", "new", "prematch_chose")
        game.customTagsList shouldBe listOf("prematch_chose", "new")
        dbRead { CasinoGame.findByIdentity("sweet_bonanza")!!.tags } shouldBe listOf("slots", "new")

        service.findAll(CasinoGameFilter.newBuilder().addTags("prematch_chose").build(), page).itemsList.map { it.identity } shouldBe listOf("sweet_bonanza")
        service.tags(page).items shouldBe listOf("new", "prematch_chose", "slots")
    }

    test("a local image overrides the hub's per key and the hub's stays underneath") {
        service.updateImage("gates_of_olympus", "1x1", "https://ours/1x1.webp")
        service.updateImage("gates_of_olympus", "4x3", "https://ours/4x3.webp")

        service.find("gates_of_olympus").item.imagesMap shouldBe mapOf("1x1" to "https://ours/1x1.webp", "4x3" to "https://ours/4x3.webp")
        dbRead { CasinoGame.findByIdentity("gates_of_olympus")!!.images } shouldBe mapOf("1x1" to "https://cdn/1x1.webp")
    }

    test("hot and cold buckets exclude the unmeasured and the inactive") {
        dbTransaction { CasinoGame.findByIdentity("sweet_bonanza")!!.active = false }

        service.findAllActiveRtp(RtpType.HOT, noFilter, page).itemsList.map { it.identity } shouldBe listOf("gates_of_olympus")
        service.findAllActiveRtp(RtpType.COLD, noFilter, page).itemsList shouldBe emptyList()
        service.find("book_of_ra").item.rtp shouldBe 0.0
    }

    test("favourites are idempotent and listed newest first; last played derives from rounds") {
        service.addFavourite("book_of_ra", "7")
        service.addFavourite("gates_of_olympus", "7")
        service.addFavourite("gates_of_olympus", "7")

        service.favourites("7", noFilter, page).itemsList.map { it.identity } shouldBe listOf("gates_of_olympus", "book_of_ra")

        service.removeFavourite("book_of_ra", "7")
        service.removeFavourite("book_of_ra", "7")
        service.favourites("7", noFilter, page).itemsList.map { it.identity } shouldBe listOf("gates_of_olympus")

        service.lastPlayed("7", page).itemsList shouldBe emptyList()
    }

    test("save is update-only and touches only the operator's fields") {
        service.save("book_of_ra", bonusBetEnable = false, bonusWageringEnable = false, active = false, order = 9)

        val game = service.find("book_of_ra").item
        game.bonusBetEnable shouldBe false
        game.active shouldBe false
        game.order shouldBe 9
        game.name shouldBe "Book of Ra"
    }

    test("batch keeps catalog order and denormalizes providers once") {
        val result = service.batch(listOf("sweet_bonanza", "gates_of_olympus", "missing"))

        result.itemsList.map { it.identity } shouldBe listOf("gates_of_olympus", "sweet_bonanza")
        result.providersList.size shouldBe 1
    }
})
