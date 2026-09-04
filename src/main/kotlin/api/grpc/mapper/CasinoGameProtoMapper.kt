package api.grpc.mapper

import api.grpc.mapper.PlatformProtoMapper.toProto
import com.nekgamebling.game.v1.CasinoGameDto
import com.nekgamebling.game.v1.casinoGameDto
import domain.model.CasinoGame

object CasinoGameProtoMapper {

    /**
     * `symbol`/`integration` are kept on the wire for backward compatibility with existing
     * consumers (backoffice-app, casino-app, telegram-miniapp) that read them, but no longer carry
     * vendor meaning — casino-engine never talks to a vendor directly any more, only to the hub, by
     * [CasinoGame.identity]. `images` is the resolved (hub + local override) merge, never the raw
     * synced set.
     */
    fun CasinoGame.toProto(): CasinoGameDto = casinoGameDto {
        identity = this@toProto.identity.value
        name = this@toProto.name
        providerIdentity = this@toProto.provider.identity.value
        collectionIdentities.addAll(this@toProto.collections.map { it.identity.value })
        bonusBetEnable = this@toProto.bonusBetEnable
        bonusWageringEnable = this@toProto.bonusWageringEnable
        tags.addAll(this@toProto.tags)
        active = this@toProto.active
        images.putAll(this@toProto.resolvedImages().data)
        order = this@toProto.order
        rtp = this@toProto.rtp ?: 0.0
        symbol = this@toProto.identity.value
        integration = "GAMEHUB"
        freeSpinEnable = this@toProto.freeSpinEnable
        freeChipEnable = this@toProto.freeChipEnable
        jackpotEnable = this@toProto.jackpotEnable
        demoEnable = this@toProto.demoEnable
        bonusBuyEnable = this@toProto.bonusBuyEnable
        locales.addAll(this@toProto.locales.map { it.value })
        platforms.addAll(this@toProto.platforms.map { it.toProto() })
        playLines = this@toProto.playLines
    }
}
