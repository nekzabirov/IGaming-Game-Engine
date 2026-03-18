package api.grpc.service

import api.grpc.config.handleGrpcCall
import api.grpc.mapper.AggregatorProtoMapper.toDomainMap
import api.grpc.mapper.AggregatorProtoMapper.toProto
import application.cqrs.Bus
import com.nekgamebling.game.v1.AggregatorDto
import com.nekgamebling.game.v1.AggregatorServiceGrpcKt
import com.nekgamebling.game.v1.Empty
import com.nekgamebling.game.v1.FindAllAggregatorResult
import com.nekgamebling.game.v1.findAllAggregatorResult
import domain.vo.Identity
import domain.vo.Pageable
import io.grpc.Status
import io.grpc.StatusException
import com.nekgamebling.game.v1.FindAggregatorQuery as FindAggregatorProto
import com.nekgamebling.game.v1.FindAllAggregatorQuery as FindAllAggregatorProto
import application.cqrs.aggregator.FindAggregatorQuery as FindAggregatorCqrs
import application.cqrs.aggregator.FindAllAggregatorQuery as FindAllAggregatorCqrs
import application.cqrs.aggregator.SaveAggregatorCommand as SaveAggregatorCqrs

class AggregatorGrpcService(
    private val bus: Bus,
) : AggregatorServiceGrpcKt.AggregatorServiceCoroutineImplBase() {

    override suspend fun save(request: AggregatorDto): Empty = handleGrpcCall {
        bus(
            SaveAggregatorCqrs(
                identity = Identity(request.identity),
                config = if (request.hasConfig()) request.config.toDomainMap() else emptyMap(),
                active = request.active,
                integration = request.integration,
            )
        )
        Empty.getDefaultInstance()
    }

    override suspend fun find(request: FindAggregatorProto): AggregatorDto = handleGrpcCall {
        val aggregator = bus(FindAggregatorCqrs(identity = Identity(request.identity)))
            .orElseThrow { StatusException(Status.NOT_FOUND.withDescription("Aggregator not found")) }

        aggregator.toProto()
    }

    override suspend fun findAll(request: FindAllAggregatorProto): FindAllAggregatorResult = handleGrpcCall {
        val page = bus(
            FindAllAggregatorCqrs(
                query = request.query,
                integration = if (request.hasIntegration()) request.integration else null,
                active = if (request.hasActive()) request.active else null,
                pageable = Pageable(request.pageNum, request.pageSize),
            )
        )

        findAllAggregatorResult {
            items.addAll(page.items.map { it.toProto() })
            totalItems = page.totalItems.toInt()
        }
    }
}
