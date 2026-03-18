package infrastructure.persistence.table

import domain.model.SpinType
import org.jetbrains.exposed.dao.id.LongIdTable

object SpinTable : LongIdTable("spins") {
    val externalId = varchar("external_id", 255).index()
    val round = reference("round_id", RoundTable).index()
    val reference = reference("reference_id", SpinTable).nullable()
    val type = enumerationByName<SpinType>("type", 20)
    val amount = long("amount")
    val realAmount = long("real_amount")
    val bonusAmount = long("bonus_amount")
}
