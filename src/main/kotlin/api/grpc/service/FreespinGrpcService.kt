package api.grpc.service

import api.grpc.config.handleGrpcCall
import api.grpc.mapper.AggregatorProtoMapper.toDomainMap
import api.grpc.mapper.AggregatorProtoMapper.toProtoStruct
import application.cqrs.Bus
import application.cqrs.freespin.GetFreespinPresetsQuery
import com.nekgamebling.game.v1.Empty
import com.nekgamebling.game.v1.FreespinServiceGrpcKt
import com.nekgamebling.game.v1.GetFreespinPresetQuery
import com.nekgamebling.game.v1.GetFreespinPresetQueryKt
import domain.vo.Currency
import domain.vo.Identity
import domain.vo.PlayerId
import kotlinx.datetime.LocalDateTime
import com.nekgamebling.game.v1.CancelFreespinCommand as CancelFreespinProto
import com.nekgamebling.game.v1.CreateFreespinCommand as CreateFreespinProto
import application.cqrs.freespin.CancelFreespinCommand as CancelFreespinCqrs
import application.cqrs.freespin.CreateFreespinCommand as CreateFreespinCqrs

class FreespinGrpcService(
    private val bus: Bus,
) : FreespinServiceGrpcKt.FreespinServiceCoroutineImplBase() {

    override suspend fun getPreset(request: GetFreespinPresetQuery): GetFreespinPresetQuery.Result = handleGrpcCall {
        val preset = bus(
            GetFreespinPresetsQuery(gameIdentity = Identity(request.gameIdentity))
        )

        GetFreespinPresetQueryKt.result {
            this.preset = preset.toProtoStruct()
        }
    }

    override suspend fun create(request: CreateFreespinProto): Empty = handleGrpcCall {
        bus(
            CreateFreespinCqrs(
                gameIdentity = Identity(request.gameIdentity),
                playerId = PlayerId(request.playerId),
                referenceId = request.referenceId,
                currency = Currency(request.currency),
                startAt = LocalDateTime.parse(request.startAt),
                endAt = LocalDateTime.parse(request.endAt),
                presetValues = if (request.hasPresetValues()) request.presetValues.toDomainMap() else emptyMap(),
            )
        )
        Empty.getDefaultInstance()
    }

    override suspend fun cancel(request: CancelFreespinProto): Empty = handleGrpcCall {
        bus(
            CancelFreespinCqrs(
                gameIdentity = Identity(request.gameIdentity),
                referenceId = request.referenceId,
            )
        )
        Empty.getDefaultInstance()
    }
}
