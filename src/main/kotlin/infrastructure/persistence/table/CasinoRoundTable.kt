package infrastructure.persistence.table

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object CasinoRoundTable : LongIdTable("casino_rounds") {
    // The hub's own round id — globally unique on its side already, so a plain unique index (not
    // a composite with anything else) is what makes two legs of one round resolve to the same row.
    val externalId = varchar("external_id", 255).uniqueIndex()
    val freespinId = varchar("freespin_id", 255).nullable()
    val playerId = varchar("player_id", 255).index()

    // Null means a sportsbook leg — the hub sends an empty `game` for those.
    val game = reference("game_id", CasinoGameTable).nullable()

    val currency = varchar("currency", 10)
    val createdAt = timestamp("created_at")
    val finishedAt = timestamp("finished_at").nullable()
}
