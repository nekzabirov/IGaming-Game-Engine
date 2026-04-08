package infrastructure.persistence.table

import org.jetbrains.exposed.sql.Table

object GameCollectionTable : Table("game_collections") {
    val game = reference("game_id", GameTable)
    val collection = reference("collection_id", CollectionTable)

    // Per-collection display position. Lower value comes first. Managed by
    // `CollectionService.AddGame` / `RemoveGame` / `UpdateGameOrder`. New
    // rows inserted from any other path (e.g. Exposed DAO relationship
    // maintenance) inherit the DB default.
    val sortOrder = integer("sort_order").default(100)

    override val primaryKey = PrimaryKey(game, collection)

    init {
        index(false, collection)
    }
}
