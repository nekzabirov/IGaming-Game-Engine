package infrastructure.api.grpc.service

import application.port.inbound.CommandHandler
import application.port.inbound.QueryHandler
import com.nekgamebling.application.port.inbound.provider.query.FindAllProvidersQuery
import com.nekgamebling.application.port.inbound.provider.query.FindAllProvidersResponse
import com.nekgamebling.application.port.inbound.provider.query.FindaProviderQuery
import com.nekgamebling.application.port.inbound.provider.query.FindaProviderResponse
import com.nekgamebling.application.port.inbound.provider.command.UpdateProviderCommand
import com.nekgamebling.application.port.inbound.provider.command.UpdateProviderImageCommand
import com.nekgamebling.game.v1.PaginationMetaDto
import com.nekgamebling.game.v1.FindAllProviderResult
import com.nekgamebling.game.v1.FindProviderResult
import com.nekgamebling.game.v1.ProviderItemDto
import com.nekgamebling.game.v1.ProviderServiceGrpcKt
import com.nekgamebling.game.v1.UpdateProviderResult
import com.nekgamebling.game.v1.UpdateProviderImageResult
import infrastructure.api.grpc.error.mapOrThrowGrpc
import infrastructure.api.grpc.mapper.toProto
import shared.value.Pageable
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import com.nekgamebling.game.v1.FindProviderQuery as FindProviderQueryProto
import com.nekgamebling.game.v1.FindAllProviderQuery as FindAllProviderQueryProto
import com.nekgamebling.game.v1.UpdateProviderCommand as UpdateProviderCommandProto
import com.nekgamebling.game.v1.UpdateProviderImageCommand as UpdateProviderImageCommandProto

class ProviderGrpcService(
    private val findProviderQueryHandler: QueryHandler<FindaProviderQuery, FindaProviderResponse>,
    private val findAllProvidersQueryHandler: QueryHandler<FindAllProvidersQuery, FindAllProvidersResponse>,
    private val updateProviderCommandHandler: CommandHandler<UpdateProviderCommand, Unit>,
    private val updateProviderImageCommandHandler: CommandHandler<UpdateProviderImageCommand, Unit>,
    coroutineContext: CoroutineContext = EmptyCoroutineContext
) : ProviderServiceGrpcKt.ProviderServiceCoroutineImplBase(coroutineContext) {

    override suspend fun find(request: FindProviderQueryProto): FindProviderResult {
        val query = FindaProviderQuery(identity = request.identity)

        return findProviderQueryHandler.handle(query)
            .mapOrThrowGrpc { response ->
                FindProviderResult.newBuilder()
                    .setProvider(response.provider.toProto(response.aggregator.identity))
                    .setAggregator(response.aggregator.toProto())
                    .setActiveGames(response.activeGames)
                    .setTotalGames(response.totalGames)
                    .build()
            }
    }

    override suspend fun findAll(request: FindAllProviderQueryProto): FindAllProviderResult {
        val query = FindAllProvidersQuery(
            pageable = if (request.hasPagination()) {
                Pageable(
                    page = request.pagination.page,
                    size = request.pagination.size
                )
            } else {
                Pageable.DEFAULT
            },
            query = request.query,
            active = if (request.hasActive()) request.active else null,
            aggregatorIdentity = if (request.hasAggregatorIdentity()) request.aggregatorIdentity else null
        )

        return findAllProvidersQueryHandler.handle(query)
            .mapOrThrowGrpc { response ->
                FindAllProviderResult.newBuilder()
                    .addAllItems(response.result.items.map { item ->
                        ProviderItemDto.newBuilder()
                            .setProvider(item.provider.toProto(item.aggregatorIdentity))
                            .setAggregatorIdentity(item.aggregatorIdentity)
                            .setActiveGames(item.activeGames)
                            .setTotalGames(item.totalGames)
                            .build()
                    })
                    .setPagination(
                        PaginationMetaDto.newBuilder()
                            .setPage(response.result.currentPage)
                            .setSize(query.pageable.sizeReal)
                            .setTotalElements(response.result.totalItems)
                            .setTotalPages(response.result.totalPages.toInt())
                            .build()
                    )
                    .addAllAggregators(response.aggregators.map { it.toProto() })
                    .build()
            }
    }

    override suspend fun update(request: UpdateProviderCommandProto): UpdateProviderResult {
        val command = UpdateProviderCommand(
            identity = request.identity,
            active = if (request.hasActive()) request.active else null,
            order = if (request.hasOrder()) request.order else null,
            aggregatorIdentity = if (request.hasAggregatorIdentity()) request.aggregatorIdentity else null
        )

        return updateProviderCommandHandler.handle(command)
            .mapOrThrowGrpc { UpdateProviderResult.newBuilder().build() }
    }

    override suspend fun updateImage(request: UpdateProviderImageCommandProto): UpdateProviderImageResult {
        val command = UpdateProviderImageCommand(
            identity = request.identity,
            key = request.key,
            file = request.file.toByteArray(),
            extension = request.extension
        )

        return updateProviderImageCommandHandler.handle(command)
            .mapOrThrowGrpc { UpdateProviderImageResult.newBuilder().build() }
    }
}
