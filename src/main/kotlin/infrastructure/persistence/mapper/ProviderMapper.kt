package infrastructure.persistence.mapper

import domain.model.Provider
import domain.vo.Country
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
        blockedCountry = blockedCountry.map { Country(it) },
    )

    fun ResultRow.toProvider(): Provider = Provider(
        identity = Identity(this[ProviderTable.identity]),
        name = this[ProviderTable.name],
        images = ImageMap(this[ProviderTable.images].toMutableMap()),
        order = this[ProviderTable.sortOrder],
        active = this[ProviderTable.active],
        aggregator = toAggregator(),
        blockedCountry = this[ProviderTable.blockedCountry].map { Country(it) },
    )
}
