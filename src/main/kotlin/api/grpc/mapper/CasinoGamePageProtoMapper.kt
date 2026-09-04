package api.grpc.mapper

import api.grpc.mapper.CollectionProtoMapper.toProto
import api.grpc.mapper.CasinoGameProtoMapper.toProto
import api.grpc.mapper.CasinoProviderProtoMapper.toProto
import domain.model.CasinoGame
import com.nekgamebling.game.v1.CasinoGamePageDto
import com.nekgamebling.game.v1.casinoGamePageDto
import domain.vo.Page

/**
 * Shared mapping from a page of [CasinoGame]s to the wire-level [CasinoGamePageDto].
 * Used by every paged game-listing RPC (`CasinoGameService.FindAll`,
 * `CasinoGameService.FindAllPlayerFavourite`, `CollectionService.FindAllCasinoGame`) so
 * the denormalization logic lives in exactly one place.
 */
object CasinoGamePageProtoMapper {

    fun Page<CasinoGame>.toCasinoGamePageDto(): CasinoGamePageDto {
        val uniqueProviders = items
            .map { it.provider }
            .distinctBy { it.identity.value }
        val uniqueCollections = items
            .flatMap { it.collections }
            .distinctBy { it.identity.value }

        return casinoGamePageDto {
            items.addAll(this@toCasinoGamePageDto.items.map { it.toProto() })
            providers.addAll(uniqueProviders.map { it.toProto() })
            collections.addAll(uniqueCollections.map { it.toProto() })
            totalItems = this@toCasinoGamePageDto.totalItems.toInt()
        }
    }
}
