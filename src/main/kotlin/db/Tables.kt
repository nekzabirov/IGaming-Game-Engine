package db

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.json.json
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

// The schema itself is owned by Flyway (src/main/resources/db/migration); these definitions
// mirror it for Exposed and are never used to create tables.

private val stringList = ListSerializer(String.serializer())

private val stringMap = MapSerializer(String.serializer(), String.serializer())

private fun Table.jsonList(name: String): Column<List<String>> =
    json(name, { Json.encodeToString(stringList, it) }, { Json.decodeFromString(stringList, it) })

private fun Table.jsonMap(name: String): Column<Map<String, String>> =
    json(name, { Json.encodeToString(stringMap, it) }, { Json.decodeFromString(stringMap, it) })

object CasinoProviders : LongIdTable("casino_providers") {
    val identity = varchar("identity", 255).uniqueIndex()
    val name = varchar("name", 255)
    val images = jsonMap("images")
    // Local override on top of `images`, never written by the catalog sync. Wins per key on the wire.
    val customImages = jsonMap("custom_images").default(emptyMap())
    val sortOrder = integer("sort_order").default(100)
    val active = bool("active").default(false)
    val blockedCountry = jsonList("blocked_country").default(emptyList())
    val tags = jsonList("tags").default(emptyList())
    // Local editorial tags on top of `tags`, which the sync overwrites wholesale on every run.
    val customTags = jsonList("custom_tags").default(emptyList())
}

object CasinoGames : LongIdTable("casino_games") {
    val identity = varchar("identity", 255).uniqueIndex()
    val name = varchar("name", 255)
    val provider = reference("provider_id", CasinoProviders)
    val bonusBetEnable = bool("bonus_bet_enable").default(true)
    val bonusWageringEnable = bool("bonus_wagering_enable").default(true)
    val tags = jsonList("tags")
    // Null means unmeasured — the hub had no bets to score in its last window — never 0.
    val rtp = double("rtp").nullable()
    val freeSpinEnable = bool("free_spin_enable").default(false)
    val freeChipEnable = bool("free_chip_enable").default(false)
    val jackpotEnable = bool("jackpot_enable").default(false)
    val demoEnable = bool("demo_enable").default(false)
    val bonusBuyEnable = bool("bonus_buy_enable").default(false)
    val locales = jsonList("locales")
    val platforms = jsonList("platforms")
    val playLines = integer("play_lines").default(0)
    val active = bool("active")
    val images = jsonMap("images")
    val customImages = jsonMap("custom_images").default(emptyMap())
    val customTags = jsonList("custom_tags").default(emptyList())
    val sortOrder = integer("sort_order")
}

object Collections : LongIdTable("collections") {
    val identity = varchar("identity", 255).uniqueIndex()
    val name = jsonMap("name")
    val tags = jsonList("tags")
    val images = jsonMap("images")
    val active = bool("active").default(true)
    val sortOrder = integer("sort_order").default(100)
}

object CasinoGameCollections : Table("casino_game_collections") {
    val game = reference("game_id", CasinoGames)
    val collection = reference("collection_id", Collections)

    // Per-collection display position, lower first. Owned by CollectionService.AddCasinoGame /
    // RemoveCasinoGame / UpdateCasinoGameOrder only.
    val sortOrder = integer("sort_order").default(100)

    override val primaryKey = PrimaryKey(game, collection)
}

object CasinoGameFavourites : LongIdTable("casino_game_favourites") {
    val game = reference("game_id", CasinoGames)
    val playerId = varchar("player_id", 255)

    init {
        uniqueIndex(playerId, game)
    }
}

object CasinoRounds : LongIdTable("casino_rounds") {
    // The hub's own round id. Unique on its own: two legs of one round resolve to the same row
    // through this index, not through a check in code.
    val externalId = varchar("external_id", 255).uniqueIndex()
    val freespinId = varchar("freespin_id", 255).nullable()
    val playerId = varchar("player_id", 255).index()
    // Null means a sportsbook leg — the hub sends an empty `game` for those.
    val game = reference("game_id", CasinoGames).nullable()
    val currency = varchar("currency", 10)
    val createdAt = timestamp("created_at")
    val finishedAt = timestamp("finished_at").nullable()
}

object Spins : LongIdTable("spins") {
    // The hub's leg id and the idempotency key: a concurrent redelivery collides here, in
    // Postgres, instead of moving money twice.
    val externalId = varchar("external_id", 255).uniqueIndex()
    val round = reference("round_id", CasinoRounds).index()
    val reference = reference("reference_id", Spins).nullable()
    val type = enumerationByName<SpinType>("type", 20)
    val amount = long("amount")
    val realAmount = long("real_amount")
    val bonusAmount = long("bonus_amount")
}
