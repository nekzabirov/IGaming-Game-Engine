package grpc

import com.nekgamebling.game.v1.OpenSportbookCommand
import com.nekgamebling.game.v1.OpenSportbookCommandKt
import com.nekgamebling.game.v1.SportbookServiceGrpcKt
import errors.Valid
import services.LaunchService

class SportbookGrpcService(
    private val launch: LaunchService,
) : SportbookServiceGrpcKt.SportbookServiceCoroutineImplBase() {

    override suspend fun open(request: OpenSportbookCommand): OpenSportbookCommand.Result = handleGrpcCall {
        val (integration, data) = launch.openSportbook(
            playerId = if (request.hasPlayerId()) Valid.playerId(request.playerId) else null,
            currency = Valid.currency(request.currency),
            locale = Valid.locale(request.locale),
        )
        OpenSportbookCommandKt.result {
            this.integration = integration
            this.data.putAll(data)
        }
    }
}
