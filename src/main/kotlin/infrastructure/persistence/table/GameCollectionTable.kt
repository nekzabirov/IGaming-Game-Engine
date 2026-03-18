package infrastructure.persistence.table

import org.jetbrains.exposed.sql.Table

object GameCollectionTable : Table("game_collections") {
    val game = reference("game_id", GameTable)
    val collection = reference("collection_id", CollectionTable)

    override val primaryKey = PrimaryKey(game, collection)

    init {
        index(false, collection)
    }
}
