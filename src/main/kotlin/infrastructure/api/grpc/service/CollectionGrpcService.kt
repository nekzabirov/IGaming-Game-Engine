package infrastructure.api.grpc.service

import application.port.inbound.CommandHandler
import application.port.inbound.QueryHandler
import com.nekgamebling.application.port.inbound.collection.command.CreateCollectionCommand
import com.nekgamebling.application.port.inbound.collection.command.CreateCollectionResponse
import com.nekgamebling.application.port.inbound.collection.command.UpdateCollectionCommand
import com.nekgamebling.application.port.inbound.collection.command.UpdateCollectionGamesCommand
import com.nekgamebling.application.port.inbound.collection.query.FindAllCollectionsQuery
import com.nekgamebling.application.port.inbound.collection.query.FindAllCollectionsResponse
import com.nekgamebling.application.port.inbound.collection.query.FindCollectionQuery
import com.nekgamebling.application.port.inbound.collection.query.FindCollectionResponse
import com.nekgamebling.game.v1.PaginationMetaDto
import com.nekgamebling.game.v1.CollectionItemDto
import com.nekgamebling.game.v1.CollectionServiceGrpcKt
import com.nekgamebling.game.v1.CreateCollectionResult
import com.nekgamebling.game.v1.FindAllCollectionResult
import com.nekgamebling.game.v1.FindCollectionResult
import com.nekgamebling.game.v1.UpdateCollectionResult
import com.nekgamebling.game.v1.UpdateCollectionGamesResult
import shared.value.LocaleName
import infrastructure.api.grpc.error.mapOrThrowGrpc
import infrastructure.api.grpc.mapper.toProto
import shared.value.Pageable
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import com.nekgamebling.game.v1.CreateCollectionCommand as CreateCollectionCommandProto
import com.nekgamebling.game.v1.FindCollectionQuery as FindCollectionQueryProto
import com.nekgamebling.game.v1.FindAllCollectionQuery as FindAllCollectionQueryProto
import com.nekgamebling.game.v1.UpdateCollectionCommand as UpdateCollectionCommandProto
import com.nekgamebling.game.v1.UpdateCollectionGamesCommand as UpdateCollectionGamesCommandProto

class CollectionGrpcService(
    private val createCollectionCommandHandler: CommandHandler<CreateCollectionCommand, CreateCollectionResponse>,
    private val findCollectionQueryHandler: QueryHandler<FindCollectionQuery, FindCollectionResponse>,
    private val findAllCollectionsQueryHandler: QueryHandler<FindAllCollectionsQuery, FindAllCollectionsResponse>,
    private val updateCollectionCommandHandler: CommandHandler<UpdateCollectionCommand, Unit>,
    private val updateCollectionGamesCommandHandler: CommandHandler<UpdateCollectionGamesCommand, Unit>,
    coroutineContext: CoroutineContext = EmptyCoroutineContext
) : CollectionServiceGrpcKt.CollectionServiceCoroutineImplBase(coroutineContext) {

    override suspend fun create(request: CreateCollectionCommandProto): CreateCollectionResult {
        val command = CreateCollectionCommand(
            identity = request.identity,
            name = LocaleName(request.name.valuesMap),
            active = if (request.hasActive()) request.active else true,
            order = if (request.hasOrder()) request.order else 100
        )

        return createCollectionCommandHandler.handle(command)
            .mapOrThrowGrpc { response ->
                CreateCollectionResult.newBuilder()
                    .setCollection(response.collection.toProto())
                    .build()
            }
    }

    override suspend fun find(request: FindCollectionQueryProto): FindCollectionResult {
        val query = FindCollectionQuery(identity = request.identity)

        return findCollectionQueryHandler.handle(query)
            .mapOrThrowGrpc { response ->
                FindCollectionResult.newBuilder()
                    .setCollection(response.collection.toProto())
                    .setProviderCount(response.providerCount)
                    .setGameCount(response.gameCount)
                    .build()
            }
    }

    override suspend fun findAll(request: FindAllCollectionQueryProto): FindAllCollectionResult {
        val query = FindAllCollectionsQuery(
            pageable = if (request.hasPagination()) {
                Pageable(
                    page = request.pagination.page,
                    size = request.pagination.size
                )
            } else {
                Pageable.DEFAULT
            },
            query = request.query,
            active = if (request.hasActive()) request.active else null
        )

        return findAllCollectionsQueryHandler.handle(query)
            .mapOrThrowGrpc { response ->
                FindAllCollectionResult.newBuilder()
                    .addAllItems(response.result.items.map { item ->
                        CollectionItemDto.newBuilder()
                            .setCollection(item.collection.toProto())
                            .setProviderCount(item.providerCount)
                            .setGameCount(item.gameCount)
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
                    .build()
            }
    }

    override suspend fun update(request: UpdateCollectionCommandProto): UpdateCollectionResult {
        val command = UpdateCollectionCommand(
            identity = request.identity,
            active = if (request.hasActive()) request.active else null,
            order = if (request.hasOrder()) request.order else null
        )

        return updateCollectionCommandHandler.handle(command)
            .mapOrThrowGrpc { UpdateCollectionResult.newBuilder().build() }
    }

    override suspend fun updateGames(request: UpdateCollectionGamesCommandProto): UpdateCollectionGamesResult {
        val command = UpdateCollectionGamesCommand(
            identity = request.identity,
            addGames = request.addGamesList,
            removeGames = request.removeGamesList
        )

        return updateCollectionGamesCommandHandler.handle(command)
            .mapOrThrowGrpc { UpdateCollectionGamesResult.newBuilder().build() }
    }
}
