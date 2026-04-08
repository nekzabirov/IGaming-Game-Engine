package api.grpc.mapper

import application.query.game.GameFilter
import domain.vo.Identity
import com.nekgamebling.game.v1.GameFilter as GameFilterProto

object GameFilterProtoMapper {

    fun GameFilterProto.toDomain(): GameFilter = GameFilter(
        query = query,
        provider = if (hasProviderIdentity()) Identity(providerIdentity) else null,
        inTags = tagsList,
        bonusBetEnable = if (hasBonusBetEnable()) bonusBetEnable else null,
        bonusWageringEnabled = if (hasBonusWageringEnable()) bonusWageringEnable else null,
        active = if (hasActive()) active else null,
        freeSpinEnable = if (hasFreeSpinEnable()) freeSpinEnable else null,
        freeChipEnable = if (hasFreeChipEnable()) freeChipEnable else null,
        jackpotEnable = if (hasJackpotEnable()) jackpotEnable else null,
        demoEnable = if (hasDemoEnable()) demoEnable else null,
        bonusBuyEnable = if (hasBonusBuyEnable()) bonusBuyEnable else null,
    )
}
