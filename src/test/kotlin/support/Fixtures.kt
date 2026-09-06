package support

import db.CasinoGame
import db.CasinoProvider
import db.Collection
import db.Platform
import plugins.dbTransaction

object Fixtures {

    suspend fun provider(
        identity: String = "test_provider",
        active: Boolean = true,
        tags: List<String> = emptyList(),
    ): Long = dbTransaction {
        CasinoProvider.new {
            this.identity = identity
            this.name = "Test Provider"
            this.images = emptyMap()
            this.active = active
            this.tags = tags
        }.id.value
    }

    suspend fun game(
        identity: String = "test_game",
        providerId: Long,
        name: String = "Test Game",
        active: Boolean = true,
        bonusBetEnable: Boolean = true,
        platforms: List<Platform> = listOf(Platform.DESKTOP, Platform.MOBILE),
        locales: List<String> = listOf("en"),
        tags: List<String> = emptyList(),
        rtp: Double? = null,
        order: Int = 0,
    ): Long = dbTransaction {
        CasinoGame.new {
            this.identity = identity
            this.name = name
            this.provider = CasinoProvider[providerId]
            this.bonusBetEnable = bonusBetEnable
            this.tags = tags
            this.rtp = rtp
            this.freeSpinEnable = true
            this.demoEnable = true
            this.locales = locales
            this.platforms = platforms.map { it.name }
            this.playLines = 20
            this.active = active
            this.images = mapOf("1x1" to "https://cdn/1x1.webp")
            this.sortOrder = order
        }.id.value
    }

    suspend fun collection(identity: String = "popular", active: Boolean = true, order: Int = 100): Long = dbTransaction {
        Collection.new {
            this.identity = identity
            this.name = mapOf("en" to identity)
            this.tags = emptyList()
            this.images = emptyMap()
            this.active = active
            this.sortOrder = order
        }.id.value
    }
}
