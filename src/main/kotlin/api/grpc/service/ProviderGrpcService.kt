package api.grpc.service

import api.grpc.config.handleGrpcCall
import api.grpc.mapper.AggregatorProtoMapper.toProto
import api.grpc.mapper.ProviderProtoMapper.toProto
import application.cqrs.Bus
import application.cqrs.provider.SaveProviderCommand
import application.cqrs.provider.SetProviderImageCommand
import com.nekgamebling.game.v1.BatchProviderQueryKt
import com.nekgamebling.game.v1.Empty
import com.nekgamebling.game.v1.FindAllProviderQueryKt
import com.nekgamebling.game.v1.FindProviderQueryKt
import com.nekgamebling.game.v1.ProviderDto
import com.nekgamebling.game.v1.ProviderServiceGrpcKt
import com.nekgamebling.game.v1.UpdateProviderImageCommand
import domain.vo.FileUpload
import domain.vo.Identity
import domain.vo.Pageable
import io.grpc.Status
import io.grpc.StatusException
import com.nekgamebling.game.v1.BatchProviderQuery as BatchProviderProto
import com.nekgamebling.game.v1.FindAllProviderQuery as FindAllProviderProto
import com.nekgamebling.game.v1.FindProviderQuery as FindProviderProto
import application.cqrs.provider.BatchProviderQuery as BatchProviderCqrs
import application.cqrs.provider.FindAllProviderQuery as FindAllProviderCqrs
import application.cqrs.provider.FindProviderQuery as FindProviderCqrs

class ProviderGrpcService(
    private val bus: Bus,
) : ProviderServiceGrpcKt.ProviderServiceCoroutineImplBase() {

    override suspend fun save(request: ProviderDto): Empty = handleGrpcCall {
        bus(
            SaveProviderCommand(
                identity = Identity(request.identity),
                name = request.name,
                order = request.order,
                active = request.active,
                aggregatorIdentity = Identity(request.aggregatorIdentity),
            )
        )
        Empty.getDefaultInstance()
    }

    override suspend fun find(request: FindProviderProto): FindProviderProto.Result = handleGrpcCall {
        val item = bus(FindProviderCqrs(identity = Identity(request.identity)))
            .orElseThrow { StatusException(Status.NOT_FOUND.withDescription("Provider not found")) }

        FindProviderQueryKt.result {
            this.item = item.provider.toProto()
            aggregator = item.provider.aggregator.toProto()
            activeGameCount = item.gameActiveCount.toInt()
            deactivateGameCount = item.gameDeactivateCount.toInt()
        }
    }

    override suspend fun findAll(request: FindAllProviderProto): FindAllProviderProto.Result = handleGrpcCall {
        val page = bus(
            FindAllProviderCqrs(
                query = request.query,
                active = if (request.hasActive()) request.active else null,
                aggregatorId = if (request.hasAggregatorIdentity()) request.aggregatorIdentity else null,
                pageable = Pageable(request.pageNum, request.pageSize),
            )
        )

        val uniqueAggregators = page.items
            .map { it.provider.aggregator }
            .distinctBy { it.identity.value }

        FindAllProviderQueryKt.result {
            items.addAll(page.items.map { providerItem ->
                FindAllProviderQueryKt.ResultKt.item {
                    provider = providerItem.provider.toProto()
                    activeGameCount = providerItem.gameActiveCount.toInt()
                    deactivateGameCount = providerItem.gameDeactivateCount.toInt()
                }
            })
            aggregators.addAll(uniqueAggregators.map { it.toProto() })
            totalItems = page.totalItems.toInt()
        }
    }

    override suspend fun batch(request: BatchProviderProto): BatchProviderProto.Result = handleGrpcCall {
        val providers = bus(BatchProviderCqrs(
            identities = request.identitiesList.map { Identity(it) },
        ))

        val uniqueAggregators = providers.map { it.aggregator }.distinctBy { it.identity.value }

        BatchProviderQueryKt.result {
            items.addAll(providers.map { provider ->
                BatchProviderQueryKt.ResultKt.item {
                    this.provider = provider.toProto()
                }
            })
            aggregators.addAll(uniqueAggregators.map { it.toProto() })
        }
    }

    override suspend fun updateImage(request: UpdateProviderImageCommand): Empty = handleGrpcCall {
        bus(
            SetProviderImageCommand(
                identity = Identity(request.identity),
                key = request.key,
                file = FileUpload(
                    name = "image.${request.extension}",
                    content = request.file.toByteArray(),
                ),
            )
        )
        Empty.getDefaultInstance()
    }
}
