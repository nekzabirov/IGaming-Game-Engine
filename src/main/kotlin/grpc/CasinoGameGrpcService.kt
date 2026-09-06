package grpc

import com.nekgamebling.game.v1.BatchCasinoGameQuery
import com.nekgamebling.game.v1.CasinoGameFavouriteCommand
import com.nekgamebling.game.v1.CasinoGamePageDto
import com.nekgamebling.game.v1.CasinoGameServiceGrpcKt
import com.nekgamebling.game.v1.Empty
import com.nekgamebling.game.v1.FindAllActiveRtpCasinoGameQuery
import com.nekgamebling.game.v1.FindAllCasinoGamePlayerFavouriteQuery
import com.nekgamebling.game.v1.FindAllCasinoGamePlayerLastQuery
import com.nekgamebling.game.v1.FindAllCasinoGameQuery
import com.nekgamebling.game.v1.FindAllCasinoGameTagQuery
import com.nekgamebling.game.v1.FindAllCasinoGameTagQueryKt
import com.nekgamebling.game.v1.FindCasinoGameQuery
import com.nekgamebling.game.v1.OpenDemoQuery
import com.nekgamebling.game.v1.OpenDemoQueryKt
import com.nekgamebling.game.v1.PlayCasinoGameCommand
import com.nekgamebling.game.v1.PlayCasinoGameCommandKt
import com.nekgamebling.game.v1.SaveCasinoGameCommand
import com.nekgamebling.game.v1.UpdateCasinoGameImageCommand
import com.nekgamebling.game.v1.UpdateCasinoGameTagsCommand
import dto.pageable
import dto.toPlatform
import errors.UnspecifiedRtpTypeException
import errors.Valid
import services.GameService
import services.LaunchService
import services.RtpType

class CasinoGameGrpcService(
    private val games: GameService,
    private val launch: LaunchService,
) : CasinoGameServiceGrpcKt.CasinoGameServiceCoroutineImplBase() {

    override suspend fun save(request: SaveCasinoGameCommand): Empty = handleGrpcCall {
        games.save(
            identity = Valid.identity(request.identity),
            bonusBetEnable = request.bonusBetEnable,
            bonusWageringEnable = request.bonusWageringEnable,
            active = request.active,
            order = request.order,
        )
        Empty.getDefaultInstance()
    }

    override suspend fun find(request: FindCasinoGameQuery): FindCasinoGameQuery.Result = handleGrpcCall {
        games.find(Valid.identity(request.identity))
    }

    override suspend fun findAll(request: FindAllCasinoGameQuery): CasinoGamePageDto = handleGrpcCall {
        games.findAll(request.filter, pageable(request.pageNum, request.pageSize))
    }

    override suspend fun findAllActiveRtp(request: FindAllActiveRtpCasinoGameQuery): CasinoGamePageDto = handleGrpcCall {
        val type = when (request.type) {
            FindAllActiveRtpCasinoGameQuery.Type.TYPE_HOT -> RtpType.HOT
            FindAllActiveRtpCasinoGameQuery.Type.TYPE_COLD -> RtpType.COLD
            else -> throw UnspecifiedRtpTypeException()
        }
        games.findAllActiveRtp(type, request.filter, pageable(request.pageNum, request.pageSize))
    }

    override suspend fun findTagsAll(request: FindAllCasinoGameTagQuery): FindAllCasinoGameTagQuery.Result = handleGrpcCall {
        val page = games.tags(pageable(request.pageNum, request.pageSize))
        FindAllCasinoGameTagQueryKt.result {
            items.addAll(page.items)
            totalItems = page.totalItems.toInt()
        }
    }

    override suspend fun batch(request: BatchCasinoGameQuery): BatchCasinoGameQuery.Result = handleGrpcCall {
        games.batch(request.identitiesList.map { Valid.identity(it) })
    }

    override suspend fun updateImage(request: UpdateCasinoGameImageCommand): Empty = handleGrpcCall {
        games.updateImage(Valid.identity(request.identity), request.key, request.url)
        Empty.getDefaultInstance()
    }

    override suspend fun updateTags(request: UpdateCasinoGameTagsCommand): Empty = handleGrpcCall {
        games.updateTags(Valid.identity(request.identity), request.tagsList)
        Empty.getDefaultInstance()
    }

    override suspend fun play(request: PlayCasinoGameCommand): PlayCasinoGameCommand.Result = handleGrpcCall {
        val url = launch.play(
            identity = Valid.identity(request.identity),
            playerId = Valid.playerId(request.playerId),
            locale = Valid.locale(request.locale),
            platform = request.platform.toPlatform(),
            currency = Valid.currency(request.currency),
            maxSpinPlaceAmount = if (request.hasMaxSpinPlaceAmount()) Valid.amount(request.maxSpinPlaceAmount) else null,
        )
        PlayCasinoGameCommandKt.result { launchUrl = url }
    }

    override suspend fun openDemo(request: OpenDemoQuery): OpenDemoQuery.Result = handleGrpcCall {
        val url = launch.demo(
            identity = Valid.identity(request.identity),
            currency = Valid.currency(request.currency),
            locale = Valid.locale(request.locale),
            platform = request.platform.toPlatform(),
            lobbyUrl = request.lobbyUrl,
        )
        OpenDemoQueryKt.result { launchUrl = url }
    }

    override suspend fun addFavourite(request: CasinoGameFavouriteCommand): Empty = handleGrpcCall {
        games.addFavourite(Valid.identity(request.identity), Valid.playerId(request.playerId))
        Empty.getDefaultInstance()
    }

    override suspend fun removeFavourite(request: CasinoGameFavouriteCommand): Empty = handleGrpcCall {
        games.removeFavourite(Valid.identity(request.identity), Valid.playerId(request.playerId))
        Empty.getDefaultInstance()
    }

    override suspend fun findAllPlayerFavourite(request: FindAllCasinoGamePlayerFavouriteQuery): CasinoGamePageDto = handleGrpcCall {
        games.favourites(Valid.playerId(request.playerId), request.filter, pageable(request.pageNum, request.pageSize))
    }

    override suspend fun findAllPlayerLast(request: FindAllCasinoGamePlayerLastQuery): CasinoGamePageDto = handleGrpcCall {
        games.lastPlayed(Valid.playerId(request.playerId), pageable(request.pageNum, request.pageSize))
    }
}
