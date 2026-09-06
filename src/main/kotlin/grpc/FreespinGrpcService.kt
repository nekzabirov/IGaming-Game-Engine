package grpc

import com.nekgamebling.game.v1.CancelFreespinCommand
import com.nekgamebling.game.v1.CreateFreespinCommand
import com.nekgamebling.game.v1.Empty
import com.nekgamebling.game.v1.FreespinServiceGrpcKt
import com.nekgamebling.game.v1.GetFreespinPresetQuery
import com.nekgamebling.game.v1.GetFreespinPresetQueryKt
import dto.toMap
import dto.toStruct
import errors.Valid
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import services.LaunchService
import kotlin.time.Duration

/** Thin proxy onto the hub: grant state lives there, casino-engine keeps no record of its own. */
class FreespinGrpcService(
    private val launch: LaunchService,
) : FreespinServiceGrpcKt.FreespinServiceCoroutineImplBase() {

    override suspend fun getPreset(request: GetFreespinPresetQuery): GetFreespinPresetQuery.Result = handleGrpcCall {
        val presets = launch.freespinPresets(Valid.identity(request.gameIdentity))
        GetFreespinPresetQueryKt.result { preset = presets.toStruct() }
    }

    override suspend fun create(request: CreateFreespinCommand): Empty = handleGrpcCall {
        launch.createFreespin(
            identity = Valid.identity(request.gameIdentity),
            playerId = Valid.playerId(request.playerId),
            reference = request.referenceId,
            currency = Valid.currency(request.currency),
            amount = request.spinAmount,
            count = request.spinCount,
            durationSeconds = window(request.startAt, request.endAt).inWholeSeconds,
            presets = if (request.hasPresetValues()) request.presetValues.toMap() else emptyMap(),
        )
        Empty.getDefaultInstance()
    }

    override suspend fun cancel(request: CancelFreespinCommand): Empty = handleGrpcCall {
        Valid.identity(request.gameIdentity)
        launch.cancelFreespin(request.referenceId)
        Empty.getDefaultInstance()
    }

    /**
     * The window arrives as local ISO dates without a zone and is read as UTC. An unreadable or
     * inverted window is no reason to refuse the grant: the hub substitutes its own default.
     */
    private fun window(startAt: String, endAt: String): Duration {
        val start = runCatching { LocalDateTime.parse(startAt).toInstant(TimeZone.UTC) }.getOrNull()
        val end = runCatching { LocalDateTime.parse(endAt).toInstant(TimeZone.UTC) }.getOrNull()

        if (start == null || end == null) return Duration.ZERO

        return (end - start).takeIf { it.isPositive() } ?: Duration.ZERO
    }
}
