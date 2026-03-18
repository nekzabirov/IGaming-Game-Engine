package infrastructure.persistence.entity

import infrastructure.persistence.table.GameFavouriteTable
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class GameFavouriteEntity(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<GameFavouriteEntity>(GameFavouriteTable)

    var gameId by GameFavouriteTable.game
    var playerId by GameFavouriteTable.playerId
}
