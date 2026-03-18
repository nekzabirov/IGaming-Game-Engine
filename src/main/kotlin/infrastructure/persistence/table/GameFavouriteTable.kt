package infrastructure.persistence.table

import org.jetbrains.exposed.dao.id.LongIdTable

object GameFavouriteTable : LongIdTable("game_favourites") {
    val game = reference("game_id", GameTable)
    val playerId = varchar("player_id", 255)

    init {
        uniqueIndex(playerId, game)
    }
}
