package grpc

import com.nekgamebling.game.v1.AddCollectionCasinoGameCommand
import com.nekgamebling.game.v1.BatchCollectionQuery
import com.nekgamebling.game.v1.BatchCollectionQueryKt
import com.nekgamebling.game.v1.CasinoGamePageDto
import com.nekgamebling.game.v1.CollectionServiceGrpcKt
import com.nekgamebling.game.v1.DeleteCollectionCommand
import com.nekgamebling.game.v1.Empty
import com.nekgamebling.game.v1.FindAllCasinoGameCollectionQuery
import com.nekgamebling.game.v1.FindAllCollectionQuery
import com.nekgamebling.game.v1.FindAllCollectionQueryKt
import com.nekgamebling.game.v1.FindCollectionQuery
import com.nekgamebling.game.v1.FindCollectionQueryKt
import com.nekgamebling.game.v1.RemoveCollectionCasinoGameCommand
import com.nekgamebling.game.v1.SaveCollectionCommand
import com.nekgamebling.game.v1.UpdateCollectionCasinoGameOrderCommand
import com.nekgamebling.game.v1.UpdateCollectionImageCommand
import dto.pageable
import errors.Valid
import services.CollectionService

class CollectionGrpcService(
    private val collections: CollectionService,
) : CollectionServiceGrpcKt.CollectionServiceCoroutineImplBase() {

    override suspend fun save(request: SaveCollectionCommand): Empty = handleGrpcCall {
        collections.save(
            identity = Valid.identity(request.identity),
            name = request.nameMap,
            tags = request.tagsList,
            active = request.active,
            order = request.order,
        )
        Empty.getDefaultInstance()
    }

    override suspend fun find(request: FindCollectionQuery): FindCollectionQuery.Result = handleGrpcCall {
        val collection = collections.find(Valid.identity(request.identity))
        FindCollectionQueryKt.result { item = collection }
    }

    override suspend fun findAll(request: FindAllCollectionQuery): FindAllCollectionQuery.Result = handleGrpcCall {
        val filter = request.filter
        val page = collections.findAll(
            query = filter.query,
            active = if (filter.hasActive()) filter.active else null,
            inTags = filter.inTagsList,
            inProviders = filter.inProviderIdentitiesList.map { Valid.identity(it) },
            pageable = pageable(request.pageNum, request.pageSize),
        )
        FindAllCollectionQueryKt.result {
            items.addAll(page.items)
            totalItems = page.totalItems.toInt()
        }
    }

    override suspend fun batch(request: BatchCollectionQuery): BatchCollectionQuery.Result = handleGrpcCall {
        val items = collections.batch(request.identitiesList.map { Valid.identity(it) })
        BatchCollectionQueryKt.result { this.items.addAll(items) }
    }

    override suspend fun findAllCasinoGame(request: FindAllCasinoGameCollectionQuery): CasinoGamePageDto = handleGrpcCall {
        collections.games(Valid.identity(request.collectionIdentity), request.filter, pageable(request.pageNum, request.pageSize))
    }

    override suspend fun addCasinoGame(request: AddCollectionCasinoGameCommand): Empty = handleGrpcCall {
        collections.addGame(Valid.identity(request.identity), Valid.identity(request.gameIdentity))
        Empty.getDefaultInstance()
    }

    override suspend fun removeCasinoGame(request: RemoveCollectionCasinoGameCommand): Empty = handleGrpcCall {
        collections.removeGame(Valid.identity(request.identity), Valid.identity(request.gameIdentity))
        Empty.getDefaultInstance()
    }

    override suspend fun updateCasinoGameOrder(request: UpdateCollectionCasinoGameOrderCommand): Empty = handleGrpcCall {
        collections.updateGameOrder(Valid.identity(request.identity), Valid.identity(request.gameIdentity), request.order)
        Empty.getDefaultInstance()
    }

    override suspend fun updateImage(request: UpdateCollectionImageCommand): Empty = handleGrpcCall {
        collections.updateImage(Valid.identity(request.identity), request.key, request.url)
        Empty.getDefaultInstance()
    }

    override suspend fun delete(request: DeleteCollectionCommand): Empty = handleGrpcCall {
        collections.delete(Valid.identity(request.identity))
        Empty.getDefaultInstance()
    }
}
