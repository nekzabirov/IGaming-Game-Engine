package infrastructure.persistence.table

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

object RoundTable : LongIdTable("rounds") {
    val externalId = varchar("external_id", 255)
    val freespinId = varchar("freespin_id", 255).nullable()
    val session = reference("session_id", SessionTable)
    val gameVariant = reference("game_variant_id", GameVariantTable)
    val createdAt = datetime("created_at")
    val finishedAt = datetime("finished_at").nullable()

    init {
        index(isUnique = true, externalId, session)
    }
}
