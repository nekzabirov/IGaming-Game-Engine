package infrastructure.api.grpc.service

import application.port.inbound.QueryHandler
import com.nekgamebling.application.port.inbound.game.query.GameItemView
import com.nekgamebling.application.port.inbound.spin.FindAllRoundQuery
import com.nekgamebling.application.port.inbound.spin.FindAllRoundQueryResult
import com.nekgamebling.application.port.inbound.spin.FindRoundQuery
import com.nekgamebling.application.port.inbound.spin.FindRoundQueryResult
import com.nekgamebling.game.v1.PaginationMetaDto
import com.nekgamebling.game.v1.FindAllRoundResult
import com.nekgamebling.game.v1.FindRoundResult
import com.nekgamebling.game.v1.GameItemDto
import com.nekgamebling.game.v1.RoundItemDto
import com.nekgamebling.game.v1.RoundServiceGrpcKt
import domain.aggregator.AggregatorInfo
import domain.provider.model.Provider
import infrastructure.api.grpc.error.mapOrThrowGrpc
import infrastructure.api.grpc.mapper.toProto
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import shared.value.Pageable
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import com.nekgamebling.game.v1.FindRoundQuery as FindRoundQueryProto
import com.nekgamebling.game.v1.FindAllRoundQuery as FindAllRoundQueryProto

class RoundGrpcService(
    private val findRoundQueryHandler: QueryHandler<FindRoundQuery, FindRoundQueryResult>,
    private val findAllRoundQueryHandler: QueryHandler<FindAllRoundQuery, FindAllRoundQueryResult>,
    coroutineContext: CoroutineContext = EmptyCoroutineContext
) : RoundServiceGrpcKt.RoundServiceCoroutineImplBase(coroutineContext) {

    private fun GameItemView.toProto(providerIdentity: String): GameItemDto =
        GameItemDto.newBuilder()
            .setGame(game.toProto(providerIdentity))
            .setActiveVariant(activeVariant.toProto(game.identity))
            .addAllCollectionIdentities(collectionIdentities)
            .build()

    override suspend fun find(request: FindRoundQueryProto): FindRoundResult {
        val query = FindRoundQuery(id = request.id)

        return findRoundQueryHandler.handle(query)
            .mapOrThrowGrpc { response ->
                val providerIdentityById = response.providers.associate { it.id to it.identity }
                val aggregatorIdentityById = response.aggregators.associate { it.id to it.identity }
                val providerIdentity = providerIdentityById[response.game.game.providerId] ?: ""

                FindRoundResult.newBuilder()
                    .setItem(
                        RoundItemDto.newBuilder()
                            .setRound(response.round.toProto())
                            .setGame(response.game.toProto(providerIdentity))
                            .setPlayerId(response.playerId)
                            .setCurrency(response.currency.value)
                            .setTotalPlaceReal(response.totalPlaceReal)
                            .setTotalPlaceBonus(response.totalPlaceBonus)
                            .setTotalSettleReal(response.totalSettleReal)
                            .setTotalSettleBonus(response.totalSettleBonus)
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

    override suspend fun findAll(request: FindAllRoundQueryProto): FindAllRoundResult {
        val query = FindAllRoundQuery(
            pageable = if (request.hasPagination()) {
                Pageable(
                    page = request.pagination.page,
                    size = request.pagination.size
                )
            } else {
                Pageable.DEFAULT
            },
            gameIdentity = if (request.hasGameIdentity()) request.gameIdentity else null,
            providerIdentity = if (request.hasProviderIdentity()) request.providerIdentity else null,
            finished = if (request.hasFinished()) request.finished else null,
            playerId = if (request.hasPlayerId()) request.playerId else null,
            freeSpinId = if (request.hasFreeSpinId()) request.freeSpinId else null,
            startAt = if (request.hasStartAt()) {
                Instant.fromEpochSeconds(request.startAt.seconds, request.startAt.nanos)
                    .toLocalDateTime(TimeZone.UTC)
            } else null,
            endAt = if (request.hasEndAt()) {
                Instant.fromEpochSeconds(request.endAt.seconds, request.endAt.nanos)
                    .toLocalDateTime(TimeZone.UTC)
            } else null,
            minPlaceAmount = if (request.hasMinPlaceAmount()) request.minPlaceAmount else null,
            maxPlaceAmount = if (request.hasMaxPlaceAmount()) request.maxPlaceAmount else null,
            minSettleAmount = if (request.hasMinSettleAmount()) request.minSettleAmount else null,
            maxSettleAmount = if (request.hasMaxSettleAmount()) request.maxSettleAmount else null
        )

        return findAllRoundQueryHandler.handle(query)
            .mapOrThrowGrpc { response ->
                val providerIdentityById = response.providers.associate { it.id to it.identity }
                val aggregatorIdentityById = response.aggregators.associate { it.id to it.identity }

                FindAllRoundResult.newBuilder()
                    .addAllItems(response.items.items.map { item ->
                        val providerIdentity = providerIdentityById[item.game.game.providerId] ?: ""
                        RoundItemDto.newBuilder()
                            .setRound(item.round.toProto())
                            .setGame(item.game.toProto(providerIdentity))
                            .setPlayerId(item.playerId)
                            .setCurrency(item.currency.value)
                            .setTotalPlaceReal(item.totalPlaceReal)
                            .setTotalPlaceBonus(item.totalPlaceBonus)
                            .setTotalSettleReal(item.totalSettleReal)
                            .setTotalSettleBonus(item.totalSettleBonus)
                            .build()
                    })
                    .setPagination(
                        PaginationMetaDto.newBuilder()
                            .setPage(response.items.currentPage)
                            .setSize(query.pageable.sizeReal)
                            .setTotalElements(response.items.totalItems)
                            .setTotalPages(response.items.totalPages.toInt())
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
}
