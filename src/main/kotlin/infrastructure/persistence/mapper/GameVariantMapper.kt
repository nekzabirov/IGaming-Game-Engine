package infrastructure.persistence.mapper

import domain.model.GameVariant
import domain.model.Platform
import domain.vo.GameSymbol
import domain.vo.Locale
import infrastructure.persistence.entity.GameVariantEntity
import infrastructure.persistence.mapper.GameMapper.toDomain

object GameVariantMapper {

    fun GameVariantEntity.toDomain(): GameVariant = GameVariant(
        id = id.value,
        symbol = GameSymbol(symbol),
        name = name,
        integration = integration,
        game = game.toDomain(),
        providerName = providerName,
        freeSpinEnable = freeSpinEnable,
        freeChipEnable = freeChipEnable,
        jackpotEnable = jackpotEnable,
        demoEnable = demoEnable,
        bonusBuyEnable = bonusBuyEnable,
        locales = locales.map { Locale(it) },
        platforms = platforms.map { Platform.valueOf(it) },
        playLines = playLines,
    )
}
