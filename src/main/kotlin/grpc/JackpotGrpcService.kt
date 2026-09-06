package grpc

import com.nekgamebling.game.v1.JackpotDto
import com.nekgamebling.game.v1.JackpotServiceGrpcKt
import com.nekgamebling.game.v1.JackpotStreamRequest
import io.grpc.Status
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** Jackpots are disabled: the hub exposes no stream. The published RPC stays, answering UNIMPLEMENTED. */
class JackpotGrpcService : JackpotServiceGrpcKt.JackpotServiceCoroutineImplBase() {

    override fun stream(request: JackpotStreamRequest): Flow<JackpotDto> = flow {
        throw Status.UNIMPLEMENTED.withDescription("jackpot streaming is not available").asRuntimeException()
    }
}
