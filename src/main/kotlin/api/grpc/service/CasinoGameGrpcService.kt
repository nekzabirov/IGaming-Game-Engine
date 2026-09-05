package api.grpc.service

import api.grpc.config.handleGrpcCall
import api.grpc.mapper.CollectionProtoMapper.toProto
import api.grpc.mapper.CasinoGameFilterProtoMapper.toDomain
import api.grpc.mapper.CasinoGamePageProtoMapper.toCasinoGamePageDto
import api.grpc.mapper.CasinoGameProtoMapper.toProto
import api.grpc.mapper.PlatformProtoMapper.toDomain
import api.grpc.mapper.CasinoProviderProtoMapper.toProto
import application.Bus
import application.command.game.AddCasinoGameFavouriteCommand
import application.query.game.BatchCasinoGameQuery
import application.query.game.FindAllActiveRtpCasinoGameQuery
import application.query.game.CasinoGameRtpType
import application.query.game.FindAllCasinoGamePlayerFavoriteQuery
import application.query.game.FindAllCasinoGamePlayerLastQuery
import application.query.game.FindAllCasinoGameQuery
import application.query.game.FindAllCasinoGameTagQuery
import application.query.game.FindCasinoGameQuery
import application.query.game.GetCasinoGameDemoUrlQuery
import application.command.game.RemoveCasinoGameFavouriteCommand
import application.command.game.SetCasinoGameImageCommand
import application.command.game.SetCasinoGameTagsCommand
import com.nekgamebling.game.v1.BatchCasinoGameQueryKt
import com.nekgamebling.game.v1.Empty
import com.nekgamebling.game.v1.FindAllCasinoGameTagQueryKt
import com.nekgamebling.game.v1.FindCasinoGameQueryKt
import com.nekgamebling.game.v1.CasinoGamePageDto
import com.nekgamebling.game.v1.CasinoGameFavouriteCommand
import com.nekgamebling.game.v1.CasinoGameServiceGrpcKt
import com.nekgamebling.game.v1.OpenDemoQuery
import com.nekgamebling.game.v1.OpenDemoQueryKt
import com.nekgamebling.game.v1.PlayCasinoGameCommandKt
import com.nekgamebling.game.v1.UpdateCasinoGameImageCommand
import com.nekgamebling.game.v1.UpdateCasinoGameTagsCommand
import domain.exception.badrequest.UnspecifiedRtpTypeException
import domain.exception.notfound.CasinoGameNotFoundException
import domain.vo.Amount
import domain.vo.Currency
import domain.vo.Identity
import domain.vo.Locale
import domain.vo.Pageable
import domain.vo.PlayerId
import com.nekgamebling.game.v1.BatchCasinoGameQuery as BatchCasinoGameProto
import com.nekgamebling.game.v1.FindAllCasinoGamePlayerFavouriteQuery as FindAllCasinoGamePlayerFavouriteProto
import com.nekgamebling.game.v1.FindAllCasinoGamePlayerLastQuery as FindAllCasinoGamePlayerLastProto
import com.nekgamebling.game.v1.FindAllActiveRtpCasinoGameQuery as FindAllActiveRtpCasinoGameProto
import com.nekgamebling.game.v1.FindAllCasinoGameQuery as FindAllCasinoGameProto
import com.nekgamebling.game.v1.FindAllCasinoGameTagQuery as FindAllCasinoGameTagProto
import com.nekgamebling.game.v1.FindCasinoGameQuery as FindCasinoGameProto
import com.nekgamebling.game.v1.PlayCasinoGameCommand as PlayCasinoGameProto
import com.nekgamebling.game.v1.SaveCasinoGameCommand as SaveCasinoGameProto
import application.command.game.PlayCasinoGameCommand as PlayCasinoGameCqrs
import application.command.game.SaveCasinoGameCommand as SaveCasinoGameCqrs

class CasinoGameGrpcService(
    private val bus: Bus,
) : CasinoGameServiceGrpcKt.CasinoGameServiceCoroutineImplBase() {

    override suspend fun save(request: SaveCasinoGameProto): Empty = handleGrpcCall {
        bus(
            SaveCasinoGameCqrs(
                identity = Identity(request.identity),
                bonusBetEnable = request.bonusBetEnable,
                bonusWageringEnable = request.bonusWageringEnable,
                active = request.active,
                order = request.order,
            )
        )
        Empty.getDefaultInstance()
    }

    override suspend fun find(request: FindCasinoGameProto): FindCasinoGameProto.Result = handleGrpcCall {
        val game = bus(FindCasinoGameQuery(identity = Identity(request.identity)))
            .orElseThrow { CasinoGameNotFoundException() }

        FindCasinoGameQueryKt.result {
            item = game.toProto()
            provider = game.provider.toProto()
            collections.addAll(game.collections.map { it.toProto() })
        }
    }

    override suspend fun findAll(request: FindAllCasinoGameProto): CasinoGamePageDto = handleGrpcCall {
        val page = bus(
            FindAllCasinoGameQuery(
                filter = request.filter.toDomain(),
                pageable = Pageable(request.pageNum, request.pageSize),
            )
        )

        page.toCasinoGamePageDto()
    }

    override suspend fun findAllActiveRtp(request: FindAllActiveRtpCasinoGameProto): CasinoGamePageDto = handleGrpcCall {
        val type = when (request.type) {
            FindAllActiveRtpCasinoGameProto.Type.TYPE_HOT -> CasinoGameRtpType.HOT
            FindAllActiveRtpCasinoGameProto.Type.TYPE_COLD -> CasinoGameRtpType.COLD
            else -> throw UnspecifiedRtpTypeException()
        }

        val page = bus(
            FindAllActiveRtpCasinoGameQuery(
                type = type,
                filter = request.filter.toDomain(),
                pageable = Pageable(request.pageNum, request.pageSize),
            )
        )

        page.toCasinoGamePageDto()
    }

    override suspend fun findTagsAll(request: FindAllCasinoGameTagProto): FindAllCasinoGameTagProto.Result = handleGrpcCall {
        val page = bus(
            FindAllCasinoGameTagQuery(
                pageable = Pageable(request.pageNum, request.pageSize),
            )
        )

        FindAllCasinoGameTagQueryKt.result {
            items.addAll(page.items)
            totalItems = page.totalItems.toInt()
        }
    }

    override suspend fun batch(request: BatchCasinoGameProto): BatchCasinoGameProto.Result = handleGrpcCall {
        val games = bus(BatchCasinoGameQuery(
            identities = request.identitiesList.map { Identity(it) },
        ))

        val uniqueProviders = games.map { it.provider }.distinctBy { it.identity.value }
        val uniqueCollections = games.flatMap { it.collections }.distinctBy { it.identity.value }

        BatchCasinoGameQueryKt.result {
            items.addAll(games.map { it.toProto() })
            providers.addAll(uniqueProviders.map { it.toProto() })
            collections.addAll(uniqueCollections.map { it.toProto() })
        }
    }

    override suspend fun updateImage(request: UpdateCasinoGameImageCommand): Empty = handleGrpcCall {
        bus(
            SetCasinoGameImageCommand(
                identity = Identity(request.identity),
                key = request.key,
                url = request.url,
            )
        )
        Empty.getDefaultInstance()
    }

    override suspend fun updateTags(request: UpdateCasinoGameTagsCommand): Empty = handleGrpcCall {
        bus(
            SetCasinoGameTagsCommand(
                identity = Identity(request.identity),
                tags = request.tagsList,
            )
        )
        Empty.getDefaultInstance()
    }

    override suspend fun play(request: PlayCasinoGameProto): PlayCasinoGameProto.Result = handleGrpcCall {
        val result = bus(
            PlayCasinoGameCqrs(
                identity = Identity(request.identity),
                playerId = PlayerId(request.playerId),
                locale = Locale(request.locale),
                platform = request.platform.toDomain(),
                currency = Currency(request.currency),
                maxSpinPlaceAmount = if (request.hasMaxSpinPlaceAmount()) Amount(request.maxSpinPlaceAmount) else null,
            )
        )

        PlayCasinoGameCommandKt.result {
            this.launchUrl = result.launchUrl
        }
    }

    override suspend fun openDemo(request: OpenDemoQuery): OpenDemoQuery.Result = handleGrpcCall {
        val launchUrl = bus(
            GetCasinoGameDemoUrlQuery(
                identity = Identity(request.identity),
                locale = Locale(request.locale),
                platform = request.platform.toDomain(),
                currency = Currency(request.currency),
                lobbyUrl = request.lobbyUrl,
            )
        )

        OpenDemoQueryKt.result {
            this.launchUrl = launchUrl
        }
    }

    override suspend fun addFavourite(request: CasinoGameFavouriteCommand): Empty = handleGrpcCall {
        bus(
            AddCasinoGameFavouriteCommand(
                identity = Identity(request.identity),
                playerId = PlayerId(request.playerId),
            )
        )
        Empty.getDefaultInstance()
    }

    override suspend fun removeFavourite(request: CasinoGameFavouriteCommand): Empty = handleGrpcCall {
        bus(
            RemoveCasinoGameFavouriteCommand(
                identity = Identity(request.identity),
                playerId = PlayerId(request.playerId),
            )
        )
        Empty.getDefaultInstance()
    }

    override suspend fun findAllPlayerFavourite(request: FindAllCasinoGamePlayerFavouriteProto): CasinoGamePageDto = handleGrpcCall {
        val page = bus(
            FindAllCasinoGamePlayerFavoriteQuery(
                playerId = PlayerId(request.playerId),
                filter = request.filter.toDomain(),
                pageable = Pageable(request.pageNum, request.pageSize),
            )
        )

        page.toCasinoGamePageDto()
    }

    override suspend fun findAllPlayerLast(request: FindAllCasinoGamePlayerLastProto): CasinoGamePageDto = handleGrpcCall {
        val page = bus(
            FindAllCasinoGamePlayerLastQuery(
                playerId = PlayerId(request.playerId),
                pageable = Pageable(request.pageNum, request.pageSize),
            )
        )

        page.toCasinoGamePageDto()
    }
}
