package api.grpc.service

import api.grpc.config.handleGrpcCall
import api.grpc.mapper.AggregatorProtoMapper.toDomainMap
import api.grpc.mapper.AggregatorProtoMapper.toProto
import application.Bus
import application.query.aggregator.BatchAggregatorQuery
import com.nekgamebling.game.v1.AggregatorServiceGrpcKt
import com.nekgamebling.game.v1.BatchAggregatorQueryKt
import com.nekgamebling.game.v1.Empty
import com.nekgamebling.game.v1.FindAggregatorQueryKt
import com.nekgamebling.game.v1.FindAllAggregatorQueryKt
import domain.exception.notfound.AggregatorNotFoundException
import domain.vo.Identity
import domain.vo.Pageable
import com.nekgamebling.game.v1.BatchAggregatorQuery as BatchAggregatorProto
import com.nekgamebling.game.v1.DeleteAggregatorCommand as DeleteAggregatorProto
import com.nekgamebling.game.v1.FindAggregatorQuery as FindAggregatorProto
import com.nekgamebling.game.v1.FindAllAggregatorQuery as FindAllAggregatorProto
import com.nekgamebling.game.v1.SaveAggregatorCommand as SaveAggregatorProto
import application.command.aggregator.DeleteAggregatorCommand as DeleteAggregatorCqrs
import application.command.aggregator.SaveAggregatorCommand as SaveAggregatorCqrs
import application.query.aggregator.FindAggregatorQuery as FindAggregatorCqrs
import application.query.aggregator.FindAllAggregatorQuery as FindAllAggregatorCqrs

class AggregatorGrpcService(
    private val bus: Bus,
) : AggregatorServiceGrpcKt.AggregatorServiceCoroutineImplBase() {

    override suspend fun save(request: SaveAggregatorProto): Empty = handleGrpcCall {
        bus(
            SaveAggregatorCqrs(
                identity = Identity(request.identity),
                integration = request.integration,
                config = if (request.hasConfig()) request.config.toDomainMap() else emptyMap(),
                active = request.active,
            )
        )
        Empty.getDefaultInstance()
    }

    override suspend fun find(request: FindAggregatorProto): FindAggregatorProto.Result = handleGrpcCall {
        val aggregator = bus(FindAggregatorCqrs(identity = Identity(request.identity)))
            .orElseThrow { AggregatorNotFoundException() }

        FindAggregatorQueryKt.result {
            item = aggregator.toProto()
        }
    }

    override suspend fun findAll(request: FindAllAggregatorProto): FindAllAggregatorProto.Result = handleGrpcCall {
        val filter = request.filter
        val page = bus(
            FindAllAggregatorCqrs(
                query = filter.query,
                integration = if (filter.hasIntegration()) filter.integration else null,
                active = if (filter.hasActive()) filter.active else null,
                pageable = Pageable(request.pageNum, request.pageSize),
            )
        )

        FindAllAggregatorQueryKt.result {
            items.addAll(page.items.map { it.toProto() })
            totalItems = page.totalItems.toInt()
        }
    }

    override suspend fun batch(request: BatchAggregatorProto): BatchAggregatorProto.Result = handleGrpcCall {
        val aggregators = bus(
            BatchAggregatorQuery(
                identities = request.identitiesList.map { Identity(it) },
            )
        )

        BatchAggregatorQueryKt.result {
            items.addAll(aggregators.map { it.toProto() })
        }
    }

    override suspend fun delete(request: DeleteAggregatorProto): Empty = handleGrpcCall {
        bus(DeleteAggregatorCqrs(identity = Identity(request.identity)))
        Empty.getDefaultInstance()
    }
}
