package api.grpc.mapper

import com.nekgamebling.game.v1.CollectionDto
import com.nekgamebling.game.v1.collectionDto
import domain.model.Collection

object CollectionProtoMapper {

    fun Collection.toProto(): CollectionDto = collectionDto {
        identity = this@toProto.identity.value
        name.putAll(this@toProto.name.data)
        images.putAll(this@toProto.images.data)
        active = this@toProto.active
        order = this@toProto.order
    }
}
