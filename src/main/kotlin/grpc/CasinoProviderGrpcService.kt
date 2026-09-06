package grpc

import com.nekgamebling.game.v1.BatchCasinoProviderQuery
import com.nekgamebling.game.v1.BatchCasinoProviderQueryKt
import com.nekgamebling.game.v1.CasinoProviderServiceGrpcKt
import com.nekgamebling.game.v1.Empty
import com.nekgamebling.game.v1.FindAllCasinoProviderQuery
import com.nekgamebling.game.v1.FindAllCasinoProviderQueryKt
import com.nekgamebling.game.v1.FindAllCasinoProviderTagQuery
import com.nekgamebling.game.v1.FindAllCasinoProviderTagQueryKt
import com.nekgamebling.game.v1.FindCasinoProviderQuery
import com.nekgamebling.game.v1.FindCasinoProviderQueryKt
import com.nekgamebling.game.v1.SaveCasinoProviderCommand
import com.nekgamebling.game.v1.UpdateCasinoProviderImageCommand
import com.nekgamebling.game.v1.UpdateCasinoProviderTagsCommand
import dto.pageable
import errors.Valid
import services.ProviderService

class CasinoProviderGrpcService(
    private val providers: ProviderService,
) : CasinoProviderServiceGrpcKt.CasinoProviderServiceCoroutineImplBase() {

    override suspend fun save(request: SaveCasinoProviderCommand): Empty = handleGrpcCall {
        providers.save(
            identity = Valid.identity(request.identity),
            order = request.order,
            active = request.active,
            blockedCountry = request.blockedCountryList.map { Valid.country(it) },
        )
        Empty.getDefaultInstance()
    }

    override suspend fun find(request: FindCasinoProviderQuery): FindCasinoProviderQuery.Result = handleGrpcCall {
        val provider = providers.find(Valid.identity(request.identity))
        FindCasinoProviderQueryKt.result { item = provider }
    }

    override suspend fun findAll(request: FindAllCasinoProviderQuery): FindAllCasinoProviderQuery.Result = handleGrpcCall {
        val filter = request.filter
        val page = providers.findAll(
            query = filter.query,
            active = if (filter.hasActive()) filter.active else null,
            inCollections = filter.inCollectionIdentitiesList.map { Valid.identity(it) },
            inTags = filter.tagsList,
            pageable = pageable(request.pageNum, request.pageSize),
        )
        FindAllCasinoProviderQueryKt.result {
            items.addAll(page.items)
            totalItems = page.totalItems.toInt()
        }
    }

    override suspend fun findTagsAll(request: FindAllCasinoProviderTagQuery): FindAllCasinoProviderTagQuery.Result = handleGrpcCall {
        val page = providers.tags(pageable(request.pageNum, request.pageSize))
        FindAllCasinoProviderTagQueryKt.result {
            items.addAll(page.items)
            totalItems = page.totalItems.toInt()
        }
    }

    override suspend fun batch(request: BatchCasinoProviderQuery): BatchCasinoProviderQuery.Result = handleGrpcCall {
        val items = providers.batch(request.identitiesList.map { Valid.identity(it) })
        BatchCasinoProviderQueryKt.result { this.items.addAll(items) }
    }

    override suspend fun updateImage(request: UpdateCasinoProviderImageCommand): Empty = handleGrpcCall {
        providers.updateImage(Valid.identity(request.identity), request.key, request.url)
        Empty.getDefaultInstance()
    }

    override suspend fun updateTags(request: UpdateCasinoProviderTagsCommand): Empty = handleGrpcCall {
        providers.updateTags(Valid.identity(request.identity), request.tagsList)
        Empty.getDefaultInstance()
    }
}
