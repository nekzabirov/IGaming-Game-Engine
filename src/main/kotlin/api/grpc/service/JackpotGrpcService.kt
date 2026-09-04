package api.grpc.service

import com.nekgamebling.game.v1.JackpotDto
import com.nekgamebling.game.v1.JackpotServiceGrpcKt
import com.nekgamebling.game.v1.JackpotStreamRequest
import io.grpc.Status
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Jackpots are disabled — casino-engine no longer talks to a vendor directly, and the hub does not
 * expose a jackpot stream. The RPC surface stays (it is an already-published contract) but answers
 * UNIMPLEMENTED rather than being removed outright.
 */
class JackpotGrpcService : JackpotServiceGrpcKt.JackpotServiceCoroutineImplBase() {

    override fun stream(request: JackpotStreamRequest): Flow<JackpotDto> = flow {
        throw Status.UNIMPLEMENTED.withDescription("jackpot streaming is not available").asRuntimeException()
    }
}
