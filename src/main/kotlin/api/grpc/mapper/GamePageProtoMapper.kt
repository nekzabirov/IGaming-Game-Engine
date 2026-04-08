package api.grpc.mapper

import api.grpc.mapper.AggregatorProtoMapper.toProto
import api.grpc.mapper.CollectionProtoMapper.toProto
import api.grpc.mapper.GameProtoMapper.toProto
import api.grpc.mapper.ProviderProtoMapper.toProto
import application.query.game.GameView
import com.nekgamebling.game.v1.GamePageDto
import com.nekgamebling.game.v1.gamePageDto
import domain.vo.Page

/**
 * Shared mapping from a page of [GameView]s to the wire-level [GamePageDto].
 * Used by every paged game-listing RPC (`GameService.FindAll`,
 * `GameService.FindAllPlayerFavourite`, `CollectionService.FindAllGame`) so
 * the denormalization logic lives in exactly one place.
 */
object GamePageProtoMapper {

    fun Page<GameView>.toGamePageDto(): GamePageDto {
        val uniqueProviders = items
            .map { it.game.provider }
            .distinctBy { it.identity.value }
        val uniqueAggregators = uniqueProviders
            .map { it.aggregator }
            .distinctBy { it.identity.value }
        val uniqueCollections = items
            .flatMap { it.game.collections }
            .distinctBy { it.identity.value }

        return gamePageDto {
            items.addAll(this@toGamePageDto.items.map { it.game.toProto(it.variant) })
            providers.addAll(uniqueProviders.map { it.toProto() })
            aggregators.addAll(uniqueAggregators.map { it.toProto() })
            collections.addAll(uniqueCollections.map { it.toProto() })
            totalItems = this@toGamePageDto.totalItems.toInt()
        }
    }
}
