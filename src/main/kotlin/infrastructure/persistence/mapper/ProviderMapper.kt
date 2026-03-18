package infrastructure.persistence.mapper

import domain.model.Provider
import domain.vo.Identity
import domain.vo.ImageMap
import infrastructure.persistence.entity.ProviderEntity
import infrastructure.persistence.mapper.AggregatorMapper.toAggregator
import infrastructure.persistence.mapper.AggregatorMapper.toDomain
import infrastructure.persistence.table.ProviderTable
import org.jetbrains.exposed.sql.ResultRow

object ProviderMapper {

    fun ProviderEntity.toDomain(): Provider = Provider(
        identity = Identity(identity),
        name = name,
        images = ImageMap(images.toMutableMap()),
        order = sortOrder,
        active = active,
        aggregator = aggregator.toDomain(),
    )

    fun ResultRow.toProvider(): Provider = Provider(
        identity = Identity(this[ProviderTable.identity]),
        name = this[ProviderTable.name],
        images = ImageMap(this[ProviderTable.images].toMutableMap()),
        order = this[ProviderTable.sortOrder],
        active = this[ProviderTable.active],
        aggregator = toAggregator(),
    )
}
