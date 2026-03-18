package api.grpc.mapper

import com.nekgamebling.game.v1.ProviderDto
import com.nekgamebling.game.v1.providerDto
import domain.model.Provider

object ProviderProtoMapper {

    fun Provider.toProto(): ProviderDto = providerDto {
        identity = this@toProto.identity.value
        name = this@toProto.name
        images.putAll(this@toProto.images.data)
        order = this@toProto.order
        active = this@toProto.active
        aggregatorIdentity = this@toProto.aggregator.identity.value
    }
}
