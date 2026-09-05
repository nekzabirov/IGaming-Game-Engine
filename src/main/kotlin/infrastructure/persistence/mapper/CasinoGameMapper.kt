package infrastructure.persistence.mapper

import domain.model.Collection
import domain.model.CasinoGame
import domain.model.Platform
import domain.vo.Identity
import domain.vo.ImageMap
import domain.vo.Locale
import infrastructure.persistence.entity.CasinoGameEntity
import infrastructure.persistence.mapper.CollectionMapper.toDomain
import infrastructure.persistence.mapper.CasinoProviderMapper.toDomain
import infrastructure.persistence.mapper.CasinoProviderMapper.toCasinoProvider
import infrastructure.persistence.table.CasinoGameTable
import org.jetbrains.exposed.sql.ResultRow

object CasinoGameMapper {

    fun CasinoGameEntity.toDomain(): CasinoGame = CasinoGame(
        identity = Identity(identity),
        name = name,
        provider = provider.toDomain(),
        collections = collections.map { it.toDomain() },
        bonusBetEnable = bonusBetEnable,
        bonusWageringEnable = bonusWageringEnable,
        tags = tags,
        rtp = rtp,
        freeSpinEnable = freeSpinEnable,
        freeChipEnable = freeChipEnable,
        jackpotEnable = jackpotEnable,
        demoEnable = demoEnable,
        bonusBuyEnable = bonusBuyEnable,
        locales = locales.map { Locale(it) },
        platforms = platforms.map { Platform.valueOf(it) },
        playLines = playLines,
        active = active,
        images = ImageMap(images.toMutableMap()),
        customImages = ImageMap(customImages.toMutableMap()),
        customTags = customTags,
        order = sortOrder,
    )

    fun ResultRow.toCasinoGame(collections: List<Collection> = emptyList()): CasinoGame = CasinoGame(
        identity = Identity(this[CasinoGameTable.identity]),
        name = this[CasinoGameTable.name],
        provider = toCasinoProvider(),
        collections = collections,
        bonusBetEnable = this[CasinoGameTable.bonusBetEnable],
        bonusWageringEnable = this[CasinoGameTable.bonusWageringEnable],
        tags = this[CasinoGameTable.tags],
        rtp = this[CasinoGameTable.rtp],
        freeSpinEnable = this[CasinoGameTable.freeSpinEnable],
        freeChipEnable = this[CasinoGameTable.freeChipEnable],
        jackpotEnable = this[CasinoGameTable.jackpotEnable],
        demoEnable = this[CasinoGameTable.demoEnable],
        bonusBuyEnable = this[CasinoGameTable.bonusBuyEnable],
        locales = this[CasinoGameTable.locales].map { Locale(it) },
        platforms = this[CasinoGameTable.platforms].map { Platform.valueOf(it) },
        playLines = this[CasinoGameTable.playLines],
        active = this[CasinoGameTable.active],
        images = ImageMap(this[CasinoGameTable.images].toMutableMap()),
        customImages = ImageMap(this[CasinoGameTable.customImages].toMutableMap()),
        customTags = this[CasinoGameTable.customTags],
        order = this[CasinoGameTable.sortOrder],
    )
}
