package api.grpc.service

import api.grpc.config.handleGrpcCall
import api.grpc.mapper.CollectionProtoMapper.toProto
import api.grpc.mapper.GameFilterProtoMapper.toDomain
import api.grpc.mapper.GamePageProtoMapper.toGamePageDto
import application.Bus
import application.command.collection.SaveCollectionCommand
import application.command.collection.SetCollectionImageCommand
import application.command.collection.UpdateCollectionGameCommand
import application.query.game.FindAllGameCollectionQuery
import com.nekgamebling.game.v1.BatchCollectionQueryKt
import com.nekgamebling.game.v1.CollectionServiceGrpcKt
import com.nekgamebling.game.v1.Empty
import com.nekgamebling.game.v1.FindAllCollectionQueryKt
import com.nekgamebling.game.v1.FindCollectionQueryKt
import com.nekgamebling.game.v1.GamePageDto
import com.nekgamebling.game.v1.UpdateCollectionGamesCommand
import com.nekgamebling.game.v1.UpdateCollectionImageCommand
import domain.exception.notfound.CollectionNotFoundException
import domain.vo.FileUpload
import domain.vo.Identity
import domain.vo.LocaleName
import domain.vo.Pageable
import com.nekgamebling.game.v1.BatchCollectionQuery as BatchCollectionProto
import com.nekgamebling.game.v1.FindAllCollectionQuery as FindAllCollectionProto
import com.nekgamebling.game.v1.FindAllGameCollectionQuery as FindAllGameCollectionProto
import com.nekgamebling.game.v1.FindCollectionQuery as FindCollectionProto
import com.nekgamebling.game.v1.SaveCollectionCommand as SaveCollectionProto
import application.query.collection.BatchCollectionQuery as BatchCollectionCqrs
import application.query.collection.FindAllCollectionQuery as FindAllCollectionCqrs
import application.query.collection.FindCollectionQuery as FindCollectionCqrs

class CollectionGrpcService(
    private val bus: Bus,
) : CollectionServiceGrpcKt.CollectionServiceCoroutineImplBase() {

    override suspend fun save(request: SaveCollectionProto): Empty = handleGrpcCall {
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
        val collection = bus(FindCollectionCqrs(identity = Identity(request.identity)))
            .orElseThrow { CollectionNotFoundException() }

        FindCollectionQueryKt.result {
            item = collection.toProto()
        }
    }

    override suspend fun findAll(request: FindAllCollectionProto): FindAllCollectionProto.Result = handleGrpcCall {
        val filter = request.filter
        val page = bus(
            FindAllCollectionCqrs(
                query = filter.query,
                active = if (filter.hasActive()) filter.active else null,
                inTags = filter.inTagsList,
                inProviderIdentities = filter.inProviderIdentitiesList.map { Identity(it) },
                pageable = Pageable(request.pageNum, request.pageSize),
            )
        )

        FindAllCollectionQueryKt.result {
            items.addAll(page.items.map { it.toProto() })
            totalItems = page.totalItems.toInt()
        }
    }

    override suspend fun batch(request: BatchCollectionProto): BatchCollectionProto.Result = handleGrpcCall {
        val collections = bus(
            BatchCollectionCqrs(
                identities = request.identitiesList.map { Identity(it) },
            )
        )

        BatchCollectionQueryKt.result {
            items.addAll(collections.map { it.toProto() })
        }
    }

    override suspend fun findAllGame(request: FindAllGameCollectionProto): GamePageDto = handleGrpcCall {
        val page = bus(
            FindAllGameCollectionQuery(
                collection = Identity(request.collectionIdentity),
                filter = request.filter.toDomain(),
                pageable = Pageable(request.pageNum, request.pageSize),
            )
        )

        page.toGamePageDto()
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
                file = FileUpload(
                    name = "image.${request.extension}",
                    content = request.file.toByteArray(),
                ),
            )
        )
        Empty.getDefaultInstance()
    }
}
