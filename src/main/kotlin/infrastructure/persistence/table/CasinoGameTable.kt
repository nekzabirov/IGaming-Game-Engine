package infrastructure.persistence.table

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.json.json

private val stringMapSerializer = MapSerializer(String.serializer(), String.serializer())
private val stringListSerializer = ListSerializer(String.serializer())

object CasinoGameTable : LongIdTable("casino_games") {
    val identity = varchar("identity", 255).uniqueIndex()
    val name = varchar("name", 255)
    val provider = reference("provider_id", CasinoProviderTable)
    val bonusBetEnable = bool("bonus_bet_enable").default(true)
    val bonusWageringEnable = bool("bonus_wagering_enable").default(true)
    val tags = json(
        "tags",
        { Json.encodeToString(stringListSerializer, it) },
        { Json.decodeFromString(stringListSerializer, it) }
    )

    // Null means unmeasured — the hub had no bets to score, never 0.
    val rtp = double("rtp").nullable()

    // Absorbed from the old casino_game_variants: one vendor now, one row, nothing left to vary.
    val freeSpinEnable = bool("free_spin_enable").default(false)
    val freeChipEnable = bool("free_chip_enable").default(false)
    val jackpotEnable = bool("jackpot_enable").default(false)
    val demoEnable = bool("demo_enable").default(false)
    val bonusBuyEnable = bool("bonus_buy_enable").default(false)
    val locales = json(
        "locales",
        { Json.encodeToString(stringListSerializer, it) },
        { Json.decodeFromString(stringListSerializer, it) }
    )
    val platforms = json(
        "platforms",
        { Json.encodeToString(stringListSerializer, it) },
        { Json.decodeFromString(stringListSerializer, it) }
    )
    val playLines = integer("play_lines").default(0)

    val active = bool("active")
    val images = json(
        "images",
        { Json.encodeToString(stringMapSerializer, it) },
        { Json.decodeFromString(stringMapSerializer, it) }
    )

    // Local-only, never written by sync. Wins per key over `images` on the wire.
    val customImages = json(
        "custom_images",
        { Json.encodeToString(stringMapSerializer, it) },
        { Json.decodeFromString(stringMapSerializer, it) }
    ).default(emptyMap())

    // Local-only, never written by sync — same deal as `custom_images`, but for `tags`, which the
    // sync overwrites wholesale from the hub on every run.
    val customTags = json(
        "custom_tags",
        { Json.encodeToString(stringListSerializer, it) },
        { Json.decodeFromString(stringListSerializer, it) }
    ).default(emptyList())

    val sortOrder = integer("sort_order")
}
