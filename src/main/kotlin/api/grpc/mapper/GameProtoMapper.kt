package api.grpc.mapper

import api.grpc.mapper.PlatformProtoMapper.toProto
import com.nekgamebling.game.v1.GameDto
import com.nekgamebling.game.v1.gameDto
import domain.model.Game
import domain.model.GameVariant

object GameProtoMapper {

    /**
     * Converts a [Game] (and its optional active [GameVariant]) into the proto DTO.
     * The variant is passed in explicitly because [Game] is a pure write-side aggregate
     * — it does not carry a back-pointer to its variant.
     */
    fun Game.toProto(variant: GameVariant? = null): GameDto = gameDto {
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
        if (variant != null) {
            symbol = variant.symbol.value
            integration = variant.integration
            freeSpinEnable = variant.freeSpinEnable
            freeChipEnable = variant.freeChipEnable
            jackpotEnable = variant.jackpotEnable
            demoEnable = variant.demoEnable
            bonusBuyEnable = variant.bonusBuyEnable
            locales.addAll(variant.locales.map { it.value })
            platforms.addAll(variant.platforms.map { it.toProto() })
            playLines = variant.playLines
        }
    }
}
