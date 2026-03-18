package infrastructure.persistence.mapper

import domain.model.Collection
import domain.vo.Identity
import domain.vo.ImageMap
import domain.vo.LocaleName
import infrastructure.persistence.entity.CollectionEntity
import infrastructure.persistence.table.CollectionTable
import org.jetbrains.exposed.sql.ResultRow

object CollectionMapper {

    fun CollectionEntity.toDomain(): Collection = Collection(
        identity = Identity(identity),
        name = LocaleName(name),
        images = ImageMap(images.toMutableMap()),
        active = active,
        order = sortOrder,
    )

    fun ResultRow.toCollection(): Collection = Collection(
        identity = Identity(this[CollectionTable.identity]),
        name = LocaleName(this[CollectionTable.name]),
        images = ImageMap(this[CollectionTable.images].toMutableMap()),
        active = this[CollectionTable.active],
        order = this[CollectionTable.sortOrder],
    )
}
