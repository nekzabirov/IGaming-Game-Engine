package api.grpc.service

import api.grpc.config.handleGrpcCall
import api.grpc.mapper.AggregatorProtoMapper.toProto
import api.grpc.mapper.ProviderProtoMapper.toProto
import application.Bus
import application.command.provider.SaveProviderCommand
import application.command.provider.SetProviderImageCommand
import com.nekgamebling.game.v1.BatchProviderQueryKt
import com.nekgamebling.game.v1.Empty
import com.nekgamebling.game.v1.FindAllProviderQueryKt
import com.nekgamebling.game.v1.FindProviderQueryKt
import com.nekgamebling.game.v1.ProviderServiceGrpcKt
import com.nekgamebling.game.v1.UpdateProviderImageCommand
import domain.exception.notfound.ProviderNotFoundException
import domain.vo.Country
import domain.vo.FileUpload
import domain.vo.Identity
import domain.vo.Pageable
import com.nekgamebling.game.v1.BatchProviderQuery as BatchProviderProto
import com.nekgamebling.game.v1.FindAllProviderQuery as FindAllProviderProto
import com.nekgamebling.game.v1.FindProviderQuery as FindProviderProto
import com.nekgamebling.game.v1.SaveProviderCommand as SaveProviderProto
import application.query.provider.BatchProviderQuery as BatchProviderCqrs
import application.query.provider.FindAllProviderQuery as FindAllProviderCqrs
import application.query.provider.FindProviderQuery as FindProviderCqrs

class ProviderGrpcService(
    private val bus: Bus,
) : ProviderServiceGrpcKt.ProviderServiceCoroutineImplBase() {

    override suspend fun save(request: SaveProviderProto): Empty = handleGrpcCall {
        bus(
            SaveProviderCommand(
                identity = Identity(request.identity),
                name = request.name,
                order = request.order,
                active = request.active,
                aggregatorIdentity = Identity(request.aggregatorIdentity),
                blockedCountry = request.blockedCountryList.map { Country(it) },
            )
        )
        Empty.getDefaultInstance()
    }

    override suspend fun find(request: FindProviderProto): FindProviderProto.Result = handleGrpcCall {
        val provider = bus(FindProviderCqrs(identity = Identity(request.identity)))
            .orElseThrow { ProviderNotFoundException() }

        FindProviderQueryKt.result {
            item = provider.toProto()
            aggregator = provider.aggregator.toProto()
        }
    }

    override suspend fun findAll(request: FindAllProviderProto): FindAllProviderProto.Result = handleGrpcCall {
        val filter = request.filter
        val page = bus(
            FindAllProviderCqrs(
                query = filter.query,
                active = if (filter.hasActive()) filter.active else null,
                aggregatorId = if (filter.hasAggregatorIdentity()) filter.aggregatorIdentity else null,
                inCollectionIdentities = filter.inCollectionIdentitiesList.map { Identity(it) },
                pageable = Pageable(request.pageNum, request.pageSize),
            )
        )

        val uniqueAggregators = page.items
            .map { it.aggregator }
            .distinctBy { it.identity.value }

        FindAllProviderQueryKt.result {
            items.addAll(page.items.map { it.toProto() })
            aggregators.addAll(uniqueAggregators.map { it.toProto() })
            totalItems = page.totalItems.toInt()
        }
    }

    override suspend fun batch(request: BatchProviderProto): BatchProviderProto.Result = handleGrpcCall {
        val providers = bus(
            BatchProviderCqrs(
                identities = request.identitiesList.map { Identity(it) },
            )
        )

        val uniqueAggregators = providers.map { it.aggregator }.distinctBy { it.identity.value }

        BatchProviderQueryKt.result {
            items.addAll(providers.map { it.toProto() })
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
