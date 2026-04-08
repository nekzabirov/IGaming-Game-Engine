package api.grpc.service

import api.grpc.config.handleGrpcCall
import api.grpc.mapper.AggregatorProtoMapper.toProto
import api.grpc.mapper.CollectionProtoMapper.toProto
import api.grpc.mapper.GameFilterProtoMapper.toDomain
import api.grpc.mapper.GameProtoMapper.toProto
import api.grpc.mapper.PlatformProtoMapper.toDomain
import api.grpc.mapper.ProviderProtoMapper.toProto
import application.Bus
import application.command.game.AddGameFavouriteCommand
import application.command.game.DeleteGameCommand
import application.query.game.BatchGameQuery
import application.query.game.FindAllGameQuery
import application.query.game.FindGameQuery
import application.query.game.GetGameDemoUrlQuery
import application.command.game.RemoveGameFavouriteCommand
import application.command.game.SetGameImageCommand
import com.nekgamebling.game.v1.BatchGameQueryKt
import com.nekgamebling.game.v1.Empty
import com.nekgamebling.game.v1.FindAllGameQueryKt
import com.nekgamebling.game.v1.FindGameQueryKt
import com.nekgamebling.game.v1.GameFavouriteCommand
import com.nekgamebling.game.v1.GameServiceGrpcKt
import com.nekgamebling.game.v1.OpenDemoQuery
import com.nekgamebling.game.v1.OpenDemoQueryKt
import com.nekgamebling.game.v1.PlayGameCommandKt
import com.nekgamebling.game.v1.UpdateGameImageCommand
import domain.exception.notfound.GameNotFoundException
import domain.vo.Amount
import domain.vo.Currency
import domain.vo.FileUpload
import domain.vo.Identity
import domain.vo.Locale
import domain.vo.Pageable
import domain.vo.PlayerId
import com.nekgamebling.game.v1.BatchGameQuery as BatchGameProto
import com.nekgamebling.game.v1.DeleteGameCommand as DeleteGameProto
import com.nekgamebling.game.v1.FindAllGameQuery as FindAllGameProto
import com.nekgamebling.game.v1.FindGameQuery as FindGameProto
import com.nekgamebling.game.v1.PlayGameCommand as PlayGameProto
import com.nekgamebling.game.v1.SaveGameCommand as SaveGameProto
import application.command.game.PlayGameCommand as PlayGameCqrs
import application.command.game.SaveGameCommand as SaveGameCqrs

class GameGrpcService(
    private val bus: Bus,
) : GameServiceGrpcKt.GameServiceCoroutineImplBase() {

    override suspend fun save(request: SaveGameProto): Empty = handleGrpcCall {
        bus(
            SaveGameCqrs(
                identity = Identity(request.identity),
                name = request.name,
                bonusBetEnable = request.bonusBetEnable,
                bonusWageringEnable = request.bonusWageringEnable,
                tags = request.tagsList,
                providerIdentity = Identity(request.providerIdentity),
            )
        )
        Empty.getDefaultInstance()
    }

    override suspend fun find(request: FindGameProto): FindGameProto.Result = handleGrpcCall {
        val view = bus(FindGameQuery(identity = Identity(request.identity)))
            .orElseThrow { GameNotFoundException() }

        FindGameQueryKt.result {
            item = view.game.toProto(view.variant)
            provider = view.game.provider.toProto()
            aggregator = view.game.provider.aggregator.toProto()
            collections.addAll(view.game.collections.map { it.toProto() })
        }
    }

    override suspend fun findAll(request: FindAllGameProto): FindAllGameProto.Result = handleGrpcCall {
        val page = bus(
            FindAllGameQuery(
                filter = request.filter.toDomain(),
                pageable = Pageable(request.pageNum, request.pageSize),
            )
        )

        val uniqueProviders = page.items.map { it.game.provider }.distinctBy { it.identity.value }
        val uniqueAggregators = uniqueProviders.map { it.aggregator }.distinctBy { it.identity.value }
        val uniqueCollections = page.items.flatMap { it.game.collections }.distinctBy { it.identity.value }

        FindAllGameQueryKt.result {
            items.addAll(page.items.map { view ->
                FindAllGameQueryKt.ResultKt.item {
                    this.game = view.game.toProto(view.variant)
                    provider = view.game.provider.toProto()
                }
            })
            providers.addAll(uniqueProviders.map { it.toProto() })
            aggregators.addAll(uniqueAggregators.map { it.toProto() })
            collections.addAll(uniqueCollections.map { it.toProto() })
            totalItems = page.totalItems.toInt()
        }
    }

    override suspend fun batch(request: BatchGameProto): BatchGameProto.Result = handleGrpcCall {
        val views = bus(BatchGameQuery(
            identities = request.identitiesList.map { Identity(it) },
        ))

        val uniqueProviders = views.map { it.game.provider }.distinctBy { it.identity.value }
        val uniqueAggregators = uniqueProviders.map { it.aggregator }.distinctBy { it.identity.value }
        val uniqueCollections = views.flatMap { it.game.collections }.distinctBy { it.identity.value }

        BatchGameQueryKt.result {
            items.addAll(views.map { view ->
                BatchGameQueryKt.ResultKt.item {
                    this.game = view.game.toProto(view.variant)
                    provider = view.game.provider.toProto()
                }
            })
            providers.addAll(uniqueProviders.map { it.toProto() })
            aggregators.addAll(uniqueAggregators.map { it.toProto() })
            collections.addAll(uniqueCollections.map { it.toProto() })
        }
    }

    override suspend fun delete(request: DeleteGameProto): Empty = handleGrpcCall {
        bus(DeleteGameCommand(identity = Identity(request.identity)))
        Empty.getDefaultInstance()
    }

    override suspend fun updateImage(request: UpdateGameImageCommand): Empty = handleGrpcCall {
        bus(
            SetGameImageCommand(
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

    override suspend fun play(request: PlayGameProto): PlayGameProto.Result = handleGrpcCall {
        val launchUrl = bus(
            PlayGameCqrs(
                identity = Identity(request.identity),
                playerId = PlayerId(request.playerId),
                locale = Locale(request.locale),
                platform = request.platform.toDomain(),
                currency = Currency(request.currency),
                maxSpinPlaceAmount = if (request.hasMaxSpinPlaceAmount()) Amount(request.maxSpinPlaceAmount) else null,
            )
        )

        PlayGameCommandKt.result {
            this.launchUrl = launchUrl
        }
    }

    override suspend fun openDemo(request: OpenDemoQuery): OpenDemoQuery.Result = handleGrpcCall {
        val launchUrl = bus(
            GetGameDemoUrlQuery(
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

    override suspend fun addFavourite(request: GameFavouriteCommand): Empty = handleGrpcCall {
        bus(
            AddGameFavouriteCommand(
                identity = Identity(request.identity),
                playerId = PlayerId(request.playerId),
            )
        )
        Empty.getDefaultInstance()
    }

    override suspend fun removeFavourite(request: GameFavouriteCommand): Empty = handleGrpcCall {
        bus(
            RemoveGameFavouriteCommand(
                identity = Identity(request.identity),
                playerId = PlayerId(request.playerId),
            )
        )
        Empty.getDefaultInstance()
    }
}
