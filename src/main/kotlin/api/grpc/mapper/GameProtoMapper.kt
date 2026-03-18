package api.grpc.mapper

import api.grpc.mapper.PlatformProtoMapper.toProto
import com.nekgamebling.game.v1.GameDto
import com.nekgamebling.game.v1.gameDto
import domain.model.Game

object GameProtoMapper {

    fun Game.toProto(): GameDto = gameDto {
        identity = this@toProto.identity.value
        name = this@toProto.name
        providerIdentity = this@toProto.provider.identity.value
        collectionIdentities.addAll(this@toProto.collections.map { it.identity.value })
        bonusBetEnable = this@toProto.bonusBetEnable
        bonusWageringEnable = this@toProto.bonusWageringEnable
        tags.addAll(this@toProto.tags)
        active = this@toProto.active
        images.putAll(this@toProto.images.data)
        order = this@toProto.order
        if (this@toProto.hasVariant) {
            val v = this@toProto.variant
            symbol = v.symbol
            integration = v.integration
            freeSpinEnable = v.freeSpinEnable
            freeChipEnable = v.freeChipEnable
            jackpotEnable = v.jackpotEnable
            demoEnable = v.demoEnable
            bonusBuyEnable = v.bonusBuyEnable
            locales.addAll(v.locales.map { it.value })
            platforms.addAll(v.platforms.map { it.toProto() })
            playLines = v.playLines
        }
    }
}
