package infrastructure.api.grpc.service

import application.port.inbound.CommandHandler
import application.port.inbound.QueryHandler
import application.port.inbound.command.AddGameTagCommand
import application.port.inbound.command.RemoveGameTagCommand
import application.port.inbound.command.UpdateGameCommand
import application.port.inbound.command.UpdateGameImageCommand
import application.service.FreespinService
import com.nekgamebling.application.port.inbound.game.command.PlayGameCommand
import com.nekgamebling.application.port.inbound.game.command.PlayGameResponse
import com.nekgamebling.application.port.inbound.game.query.FindAllGameQuery
import com.nekgamebling.application.port.inbound.game.query.FindAllGameResponse
import com.nekgamebling.application.port.inbound.game.query.FindGameQuery
import com.nekgamebling.application.port.inbound.game.query.FindGameResponse
import com.nekgamebling.application.port.inbound.game.query.GameDemoUrlQuery
import com.nekgamebling.application.port.inbound.game.query.GameDemoUrlResponse
import com.nekgamebling.game.dto.PaginationMetaDto
import com.nekgamebling.game.dto.PlatformDto
import com.nekgamebling.game.service.*
import domain.common.value.Locale
import domain.common.value.Platform
import infrastructure.api.grpc.error.GrpcErrorMapper
import infrastructure.api.grpc.error.mapOrThrowGrpc
import infrastructure.api.grpc.mapper.toDomain
import infrastructure.api.grpc.mapper.toProto
import shared.value.Currency
import shared.value.Pageable
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import com.nekgamebling.game.service.FindAllGameQuery as FindAllGameQueryProto
import com.nekgamebling.game.service.FindGameQuery as FindGameQueryProto
import com.nekgamebling.game.service.UpdateGameCommand as UpdateGameCommandProto
import com.nekgamebling.game.service.UpdateGameImageCommand as UpdateGameImageCommandProto
import com.nekgamebling.game.service.AddGameTagCommand as AddGameTagCommandProto
import com.nekgamebling.game.service.RemoveGameTagCommand as RemoveGameTagCommandProto
import com.nekgamebling.game.service.GameDemoUrlQuery as GameDemoUrlQueryProto
import com.nekgamebling.game.service.PlayGameCommand as PlayGameCommandProto

class GameGrpcService(
    private val findGameQueryHandler: QueryHandler<FindGameQuery, FindGameResponse>,
    private val findAllGameQueryHandler: QueryHandler<FindAllGameQuery, FindAllGameResponse>,
    private val gameDemoUrlQueryHandler: QueryHandler<GameDemoUrlQuery, GameDemoUrlResponse>,
    private val playGameCommandHandler: CommandHandler<PlayGameCommand, PlayGameResponse>,
    private val updateGameCommandHandler: CommandHandler<UpdateGameCommand, Unit>,
    private val updateGameImageCommandHandler: CommandHandler<UpdateGameImageCommand, Unit>,
    private val addGameTagCommandHandler: CommandHandler<AddGameTagCommand, Unit>,
    private val removeGameTagCommandHandler: CommandHandler<RemoveGameTagCommand, Unit>,
    private val freespinService: FreespinService,
    coroutineContext: CoroutineContext = EmptyCoroutineContext
) : GameServiceGrpcKt.GameServiceCoroutineImplBase(coroutineContext) {

    override suspend fun find(request: FindGameQueryProto): FindGameResult {
        val query = FindGameQuery(identity = request.identity)

        return findGameQueryHandler.handle(query)
            .mapOrThrowGrpc { response ->
                FindGameResult.newBuilder()
                    .setGame(response.game.toProto(response.provider.identity))
                    .setProvider(response.provider.toProto(response.aggregator.identity))
                    .setActiveVariant(response.activeVariant.toProto(response.game.identity))
                    .setAggregator(response.aggregator.toProto())
                    .addAllCollections(response.collections.map { it.toProto() })
                    .build()
            }
    }

    override suspend fun findAll(request: FindAllGameQueryProto): FindAllGameResult {
        val query = FindAllGameQuery(
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
            providerIdentities = request.providerIdentitiesList.takeIf { it.isNotEmpty() },
            collectionIdentities = request.collectionIdentitiesList.takeIf { it.isNotEmpty() },
            tags = request.tagsList.takeIf { it.isNotEmpty() },
            bonusBetEnable = if (request.hasBonusBetEnable()) request.bonusBetEnable else null,
            bonusWageringEnable = if (request.hasBonusWageringEnable()) request.bonusWageringEnable else null,
            freeSpinEnable = if (request.hasFreeSpinEnable()) request.freeSpinEnable else null,
            freeChipEnable = if (request.hasFreeChipEnable()) request.freeChipEnable else null,
            jackpotEnable = if (request.hasJackpotEnable()) request.jackpotEnable else null
        )

        val result = try {
            findAllGameQueryHandler.handle(query)
        } catch (e: Exception) {
            throw GrpcErrorMapper.toStatusException(e)
        }

        return result
            .mapOrThrowGrpc { response ->
                // Build lookup map for provider identity by id
                val providerIdentityById = response.providers.associate { it.id to it.identity }
                // Build lookup map for aggregator identity by id
                val aggregatorIdentityById = response.aggregators.associate { it.id to it.identity }

                FindAllGameResult.newBuilder()
                    .addAllItems(response.result.items.map { item ->
                        val providerIdentity = providerIdentityById[item.game.providerId] ?: ""
                        GameItemDto.newBuilder()
                            .setGame(item.game.toProto(providerIdentity))
                            .setActiveVariant(item.activeVariant.toProto(item.game.identity))
                            .addAllCollectionIdentities(item.collectionIdentities)
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
                    .addAllProviders(response.providers.map { provider ->
                        val aggregatorIdentity = provider.aggregatorId?.let { aggregatorIdentityById[it] }
                        provider.toProto(aggregatorIdentity)
                    })
                    .addAllAggregators(response.aggregators.map { it.toProto() })
                    .addAllCollections(response.collections.map { it.toProto() })
                    .build()
            }
    }

    override suspend fun demoUrl(request: GameDemoUrlQueryProto): GameDemoUrlResult {
        val query = GameDemoUrlQuery(
            identity = request.identity,
            currency = Currency(request.currency),
            locale = Locale(request.locale),
            platform = request.platform.toDomain(),
            lobbyUrl = request.lobbyUrl
        )

        return gameDemoUrlQueryHandler.handle(query)
            .mapOrThrowGrpc { response ->
                GameDemoUrlResult.newBuilder()
                    .setLaunchUrl(response.launchUrl)
                    .build()
            }
    }

    private fun PlatformDto.toDomain(): Platform = when (this) {
        PlatformDto.PLATFORM_DESKTOP -> Platform.DESKTOP
        PlatformDto.PLATFORM_MOBILE -> Platform.MOBILE
        PlatformDto.PLATFORM_DOWNLOAD -> Platform.DOWNLOAD
        else -> Platform.DESKTOP
    }

    override suspend fun play(request: PlayGameCommandProto): PlayGameResult {
        val command = PlayGameCommand(
            identity = request.identity,
            playerId = request.playerId,
            currency = Currency(request.currency),
            locale = Locale(request.locale),
            platform = request.platform.toDomain(),
            lobbyUrl = request.lobbyUrl,
            spinLimitAmount = if (request.hasSpinLimitAmount()) request.spinLimitAmount else null
        )

        return playGameCommandHandler.handle(command)
            .mapOrThrowGrpc { response ->
                PlayGameResult.newBuilder()
                    .setLaunchUrl(response.launchUrl)
                    .build()
            }
    }

    override suspend fun update(request: UpdateGameCommandProto): UpdateGameResult {
        val command = UpdateGameCommand(
            identity = request.identity,
            bonusBetEnable = if (request.hasBonusBetEnable()) request.bonusBetEnable else null,
            bonusWageringEnable = if (request.hasBonusWageringEnable()) request.bonusWageringEnable else null,
            active = if (request.hasActive()) request.active else null
        )

        return updateGameCommandHandler.handle(command)
            .mapOrThrowGrpc { UpdateGameResult.newBuilder().build() }
    }

    override suspend fun updateImage(request: UpdateGameImageCommandProto): UpdateGameImageResult {
        val command = UpdateGameImageCommand(
            identity = request.identity,
            key = request.key,
            file = request.file.toByteArray(),
            extension = request.extension
        )

        return updateGameImageCommandHandler.handle(command)
            .mapOrThrowGrpc { UpdateGameImageResult.newBuilder().build() }
    }

    override suspend fun addTag(request: AddGameTagCommandProto): AddGameTagResult {
        val command = AddGameTagCommand(
            identity = request.identity,
            tag = request.tag
        )

        return addGameTagCommandHandler.handle(command)
            .mapOrThrowGrpc { AddGameTagResult.newBuilder().build() }
    }

    override suspend fun removeTag(request: RemoveGameTagCommandProto): RemoveGameTagResult {
        val command = RemoveGameTagCommand(
            identity = request.identity,
            tag = request.tag
        )

        return removeGameTagCommandHandler.handle(command)
            .mapOrThrowGrpc { RemoveGameTagResult.newBuilder().build() }
    }

    override suspend fun getFreespinPreset(request: GetFreespinPresetQuery): GetFreespinPresetResult {
        return freespinService.getPreset(request.gameIdentity)
            .mapOrThrowGrpc { result ->
                GetFreespinPresetResult.newBuilder()
                    .putAllPreset(result.preset.mapValues { it.value?.toString() ?: "" })
                    .build()
            }
    }

    override suspend fun createFreespin(request: CreateFreespinCommand): CreateFreespinResult {
        return freespinService.create(
            presetValue = request.presetValuesMap,
            referenceId = request.referenceId,
            playerId = request.playerId,
            gameIdentity = request.gameIdentity,
            currency = Currency(request.currency),
            startAt = request.startAt.toDomain(),
            endAt = request.endAt.toDomain()
        )
            .mapOrThrowGrpc { CreateFreespinResult.newBuilder().build() }
    }

    override suspend fun cancelFreespin(request: CancelFreespinCommand): CancelFreespinResult {
        return freespinService.cancel(
            referenceId = request.referenceId,
            gameIdentity = request.gameIdentity
        )
            .mapOrThrowGrpc { CancelFreespinResult.newBuilder().build() }
    }
}
