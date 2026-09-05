package infrastructure.persistence.mapper

import domain.model.CasinoProvider
import domain.vo.Country
import domain.vo.Identity
import domain.vo.ImageMap
import infrastructure.persistence.entity.CasinoProviderEntity
import infrastructure.persistence.table.CasinoProviderTable
import org.jetbrains.exposed.sql.ResultRow

object CasinoProviderMapper {

    fun CasinoProviderEntity.toDomain(): CasinoProvider = CasinoProvider(
        identity = Identity(identity),
        name = name,
        images = ImageMap(images.toMutableMap()),
        customImages = ImageMap(customImages.toMutableMap()),
        order = sortOrder,
        active = active,
        blockedCountry = blockedCountry.map { Country(it) },
        tags = tags,
        customTags = customTags,
    )

    fun ResultRow.toCasinoProvider(): CasinoProvider = CasinoProvider(
        identity = Identity(this[CasinoProviderTable.identity]),
        name = this[CasinoProviderTable.name],
        images = ImageMap(this[CasinoProviderTable.images].toMutableMap()),
        customImages = ImageMap(this[CasinoProviderTable.customImages].toMutableMap()),
        order = this[CasinoProviderTable.sortOrder],
        active = this[CasinoProviderTable.active],
        blockedCountry = this[CasinoProviderTable.blockedCountry].map { Country(it) },
        tags = this[CasinoProviderTable.tags],
        customTags = this[CasinoProviderTable.customTags],
    )
}
