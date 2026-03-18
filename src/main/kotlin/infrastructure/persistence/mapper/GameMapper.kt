package infrastructure.persistence.mapper

import domain.model.Collection
import domain.model.Game
import domain.vo.Identity
import domain.vo.ImageMap
import infrastructure.persistence.entity.GameEntity
import infrastructure.persistence.mapper.CollectionMapper.toDomain
import infrastructure.persistence.mapper.ProviderMapper.toDomain
import infrastructure.persistence.mapper.ProviderMapper.toProvider
import infrastructure.persistence.table.GameTable
import org.jetbrains.exposed.sql.ResultRow

object GameMapper {

    fun GameEntity.toDomain(): Game = Game(
        identity = Identity(identity),
        name = name,
        provider = provider.toDomain(),
        collections = collections.map { it.toDomain() },
        bonusBetEnable = bonusBetEnable,
        bonusWageringEnable = bonusWageringEnable,
        tags = tags,
        active = active,
        images = ImageMap(images.toMutableMap()),
        order = sortOrder,
    )

    fun ResultRow.toGame(collections: List<Collection> = emptyList()): Game = Game(
        identity = Identity(this[GameTable.identity]),
        name = this[GameTable.name],
        provider = toProvider(),
        collections = collections,
        bonusBetEnable = this[GameTable.bonusBetEnable],
        bonusWageringEnable = this[GameTable.bonusWageringEnable],
        tags = this[GameTable.tags],
        active = this[GameTable.active],
        images = ImageMap(this[GameTable.images].toMutableMap()),
        order = this[GameTable.sortOrder],
    )
}
