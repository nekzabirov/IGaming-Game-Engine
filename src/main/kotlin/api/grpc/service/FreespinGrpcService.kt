package api.grpc.service

import api.grpc.config.handleGrpcCall
import api.grpc.mapper.StructProtoMapper.toDomainMap
import api.grpc.mapper.StructProtoMapper.toProtoStruct
import application.Bus
import application.query.freespin.GetFreespinPresetsQuery
import com.nekgamebling.game.v1.CreateFreespinCommandKt
import com.nekgamebling.game.v1.Empty
import com.nekgamebling.game.v1.FreespinServiceGrpcKt
import com.nekgamebling.game.v1.GetFreespinPresetQuery
import com.nekgamebling.game.v1.GetFreespinPresetQueryKt
import domain.vo.Currency
import domain.vo.Identity
import domain.vo.PlayerId
import com.nekgamebling.game.v1.CancelFreespinCommand as CancelFreespinProto
import com.nekgamebling.game.v1.CreateFreespinCommand as CreateFreespinProto
import application.command.freespin.CancelFreespinCommand as CancelFreespinCqrs
import application.command.freespin.CreateFreespinCommand as CreateFreespinCqrs

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

    override suspend fun create(request: CreateFreespinProto): CreateFreespinProto.Result = handleGrpcCall {
        val id = bus(
            CreateFreespinCqrs(
                gameIdentity = Identity(request.gameIdentity),
                playerId = PlayerId(request.playerId),
                currency = Currency(request.currency),
                spinAmount = request.spinAmount,
                spinCount = request.spinCount,
                presetValues = if (request.hasPresetValues()) request.presetValues.toDomainMap() else emptyMap(),
            )
        )

        CreateFreespinCommandKt.result { this.id = id }
    }

    override suspend fun cancel(request: CancelFreespinProto): Empty = handleGrpcCall {
        bus(CancelFreespinCqrs(id = request.id))
        Empty.getDefaultInstance()
    }
}
