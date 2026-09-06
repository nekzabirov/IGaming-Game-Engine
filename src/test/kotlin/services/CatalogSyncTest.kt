package services

import db.CasinoGame
import db.CasinoProvider
import gamehub.v1.Gateway
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import plugins.dbRead
import plugins.dbTransaction
import support.FakeGameHub
import support.TestDatabase

class CatalogSyncTest : FunSpec({

    TestDatabase.connect()

    fun catalog(rtp: Double? = 96.5, name: String = "Gates of Olympus", tags: List<String> = listOf("slots")) =
        Gateway.ListCasinoResponse.newBuilder()
            .addProviders(Gateway.Provider.newBuilder().setIdentity("pragmatic").setName("Pragmatic").putImages("1x1", "https://cdn/p.webp").addTags("live"))
            .addGames(
                Gateway.Game.newBuilder()
                    .setIdentity("gates_of_olympus").setName(name).setProvider("pragmatic")
                    .putImages("1x1", "https://cdn/g.webp").addAllTags(tags)
                    .addLocales("en").addPlatforms("desktop").addPlatforms("mobile").addPlatforms("tv")
                    .setPlayLines(20).setDemoEnable(true).setFreespinEnable(true)
                    .apply { if (rtp != null) setRtp(rtp) },
            )
            .addGames(Gateway.Game.newBuilder().setIdentity("orphan").setName("Orphan").setProvider("nobody"))
            .build()

    beforeTest { TestDatabase.reset() }

    test("a first run creates providers and games active, with catalog defaults, skipping unknown providers") {
        val result = CatalogSync(FakeGameHub(catalog())).run()

        result shouldBe CatalogSync.Result(providers = 1, games = 1)
        dbRead {
            val provider = CasinoProvider.findByIdentity("pragmatic")!!
            provider.active shouldBe true
            provider.sortOrder shouldBe 100
            provider.tags shouldBe listOf("live")

            val game = CasinoGame.findByIdentity("gates_of_olympus")!!
            game.active shouldBe true
            game.sortOrder shouldBe 0
            game.rtp shouldBe 96.5
            game.platforms shouldBe listOf("DESKTOP", "MOBILE")
            game.freeSpinEnable shouldBe true
            game.freeChipEnable shouldBe false
            game.provider.identity shouldBe "pragmatic"
            CasinoGame.findByIdentity("orphan").shouldBeNull()
        }
    }

    test("a second run overwrites the hub's fields and leaves ours alone, keeping rtp when the hub has none") {
        CatalogSync(FakeGameHub(catalog())).run()
        dbTransaction {
            CasinoProvider.findByIdentity("pragmatic")!!.apply {
                active = false
                sortOrder = 5
                customImages = mapOf("1x1" to "https://ours/p.webp")
                customTags = listOf("prematch_chose")
                blockedCountry = listOf("RU")
            }
            CasinoGame.findByIdentity("gates_of_olympus")!!.apply {
                active = false
                sortOrder = -50
                bonusBetEnable = false
                customTags = listOf("prematch_chose")
                customImages = mapOf("4x3" to "https://ours/g.webp")
            }
        }

        CatalogSync(FakeGameHub(catalog(rtp = null, name = "Gates of Olympus 1000", tags = listOf("slots", "new")))).run()

        dbRead {
            val provider = CasinoProvider.findByIdentity("pragmatic")!!
            provider.active shouldBe false
            provider.sortOrder shouldBe 5
            provider.customImages shouldBe mapOf("1x1" to "https://ours/p.webp")
            provider.customTags shouldBe listOf("prematch_chose")
            provider.blockedCountry shouldBe listOf("RU")

            val game = CasinoGame.findByIdentity("gates_of_olympus")!!
            game.name shouldBe "Gates of Olympus 1000"
            game.tags shouldBe listOf("slots", "new")
            game.rtp shouldBe 96.5
            game.active shouldBe false
            game.sortOrder shouldBe -50
            game.bonusBetEnable shouldBe false
            game.customTags shouldBe listOf("prematch_chose")
            game.customImages shouldBe mapOf("4x3" to "https://ours/g.webp")
        }
    }

    test("a measured rtp replaces the old one") {
        CatalogSync(FakeGameHub(catalog(rtp = 96.5))).run()
        CatalogSync(FakeGameHub(catalog(rtp = 91.0))).run()

        dbRead { CasinoGame.findByIdentity("gates_of_olympus")!!.rtp } shouldBe 91.0
    }
})
