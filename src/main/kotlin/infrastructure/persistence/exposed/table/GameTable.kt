package infrastructure.persistence.exposed.table

import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.json.jsonb
import shared.value.ImageMap

object GameTable : BaseTable("games"), IdentityTable {
    override val identity = varchar("identity", 100).uniqueIndex()
    val name = varchar("name", 100)
    val providerId = reference("provider_id", ProviderTable.id)
    val images = jsonb<ImageMap>("images", Json.Default).default(ImageMap.EMPTY)
    val bonusBetEnable = bool("bonus_bet_enable").default(true)
    val bonusWageringEnable = bool("bonus_wagering_enable").default(true)
    val tags = array<String>("tags").default(emptyList())
    val active = bool("active").default(true)
}
