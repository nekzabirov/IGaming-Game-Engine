package api.grpc.service

import api.grpc.config.handleGrpcCall
import api.grpc.mapper.StructProtoMapper.toDomainMap
import api.grpc.mapper.StructProtoMapper.toProtoStruct
import application.Bus
import application.query.freespin.GetFreespinPresetsQuery
import com.nekgamebling.game.v1.Empty
import com.nekgamebling.game.v1.FreespinServiceGrpcKt
import com.nekgamebling.game.v1.GetFreespinPresetQuery
import com.nekgamebling.game.v1.GetFreespinPresetQueryKt
import domain.vo.Currency
import domain.vo.Identity
import domain.vo.PlayerId
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.time.Duration
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

    override suspend fun create(request: CreateFreespinProto): Empty = handleGrpcCall {
        bus(
            CreateFreespinCqrs(
                gameIdentity = Identity(request.gameIdentity),
                playerId = PlayerId(request.playerId),
                referenceId = request.referenceId,
                currency = Currency(request.currency),
                spinAmount = request.spinAmount,
                spinCount = request.spinCount,
                duration = window(request.startAt, request.endAt),
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

    /**
     * Окно приезжает локальными ISO-датами без зоны и читается как UTC — так его пишет вызывающий.
     * Нечитаемое или перевёрнутое окно не повод отказать в гранте: хаб подставит своё значение по
     * умолчанию, а грант без срока лучше, чем несозданный.
     */
    private fun window(startAt: String, endAt: String): Duration {
        val start = runCatching { LocalDateTime.parse(startAt).toInstant(TimeZone.UTC) }.getOrNull()
        val end = runCatching { LocalDateTime.parse(endAt).toInstant(TimeZone.UTC) }.getOrNull()

        if (start == null || end == null) return Duration.ZERO

        return (end - start).takeIf { it.isPositive() } ?: Duration.ZERO
    }
}
