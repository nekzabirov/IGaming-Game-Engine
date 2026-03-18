package api.grpc.service

import api.grpc.config.handleGrpcCall
import api.grpc.mapper.CollectionProtoMapper.toProto
import application.cqrs.Bus
import application.cqrs.collection.SaveCollectionCommand
import application.cqrs.collection.SetCollectionImageCommand
import application.cqrs.collection.UpdateCollectionGameCommand
import com.nekgamebling.game.v1.CollectionDto
import com.nekgamebling.game.v1.CollectionServiceGrpcKt
import com.nekgamebling.game.v1.Empty
import com.nekgamebling.game.v1.FindAllCollectionQueryKt
import com.nekgamebling.game.v1.FindCollectionQueryKt
import com.nekgamebling.game.v1.UpdateCollectionGamesCommand
import com.nekgamebling.game.v1.UpdateCollectionImageCommand
import domain.vo.Identity
import domain.vo.LocaleName
import domain.vo.Pageable
import io.grpc.Status
import io.grpc.StatusException
import com.nekgamebling.game.v1.FindAllCollectionQuery as FindAllCollectionProto
import com.nekgamebling.game.v1.FindCollectionQuery as FindCollectionProto
import application.cqrs.collection.FindAllCollectionQuery as FindAllCollectionCqrs
import application.cqrs.collection.FindCollectionQuery as FindCollectionCqrs

class CollectionGrpcService(
    private val bus: Bus,
) : CollectionServiceGrpcKt.CollectionServiceCoroutineImplBase() {

    override suspend fun save(request: CollectionDto): Empty = handleGrpcCall {
        bus(
            SaveCollectionCommand(
                identity = Identity(request.identity),
                name = LocaleName(request.nameMap),
                active = request.active,
                order = request.order,
            )
        )
        Empty.getDefaultInstance()
    }

    override suspend fun find(request: FindCollectionProto): FindCollectionProto.Result = handleGrpcCall {
        val item = bus(FindCollectionCqrs(identity = Identity(request.identity)))
            .orElseThrow { StatusException(Status.NOT_FOUND.withDescription("Collection not found")) }

        FindCollectionQueryKt.result {
            this.item = item.collection.toProto()
            gameActiveCount = item.gameActiveCount.toInt()
            gameDeactivateCount = item.gameDeactivateCount.toInt()
            providerCount = item.providerCount.toInt()
        }
    }

    override suspend fun findAll(request: FindAllCollectionProto): FindAllCollectionProto.Result = handleGrpcCall {
        val page = bus(
            FindAllCollectionCqrs(
                query = request.query,
                active = if (request.hasActive()) request.active else null,
                pageable = Pageable(request.pageNum, request.pageSize),
            )
        )

        FindAllCollectionQueryKt.result {
            items.addAll(page.items.map { collectionItem ->
                FindAllCollectionQueryKt.ResultKt.item {
                    collection = collectionItem.collection.toProto()
                    gameActiveCount = collectionItem.gameActiveCount.toInt()
                    gameDeactivateCount = collectionItem.gameDeactivateCount.toInt()
                    providerCount = collectionItem.providerCount.toInt()
                }
            })
            totalItems = page.totalItems.toInt()
        }
    }

    override suspend fun updateGames(request: UpdateCollectionGamesCommand): Empty = handleGrpcCall {
        bus(
            UpdateCollectionGameCommand(
                identity = Identity(request.identity),
                addGameIdentities = request.addGamesList.map { Identity(it) },
                deleteGameIdentities = request.removeGamesList.map { Identity(it) },
            )
        )
        Empty.getDefaultInstance()
    }

    override suspend fun updateImage(request: UpdateCollectionImageCommand): Empty = handleGrpcCall {
        bus(
            SetCollectionImageCommand(
                identity = Identity(request.identity),
                key = request.key,
                file = domain.vo.FileUpload(
                    name = "image.${request.extension}",
                    content = request.file.toByteArray(),
                ),
            )
        )
        Empty.getDefaultInstance()
    }
}
