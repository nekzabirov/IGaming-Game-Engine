package infrastructure.persistence.table

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.json.json

private val stringListSerializer = ListSerializer(String.serializer())

object GameVariantTable : LongIdTable("game_variants") {
    val symbol = varchar("symbol", 255).uniqueIndex()
    val name = varchar("name", 255)
    val integration = varchar("integration", 255)
    val game = reference("game_id", GameTable).index()
    val providerName = varchar("provider_name", 255)
    val freeSpinEnable = bool("free_spin_enable")
    val freeChipEnable = bool("free_chip_enable")
    val jackpotEnable = bool("jackpot_enable")
    val demoEnable = bool("demo_enable")
    val bonusBuyEnable = bool("bonus_buy_enable")
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
}
