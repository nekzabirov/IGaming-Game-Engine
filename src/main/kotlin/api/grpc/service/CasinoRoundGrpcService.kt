package api.grpc.service

import api.grpc.config.handleGrpcCall
import api.grpc.mapper.CasinoRoundProtoMapper.toProto
import application.Bus
import application.query.round.FindAllCasinoRoundQuery
import application.query.round.FindCasinoRoundQuery
import com.nekgamebling.game.v1.CasinoRoundServiceGrpcKt
import com.nekgamebling.game.v1.FindAllCasinoRoundQueryKt
import com.nekgamebling.game.v1.FindCasinoRoundQueryKt
import domain.exception.badrequest.InvalidDateFormatException
import domain.exception.notfound.CasinoRoundNotFoundException
import domain.vo.Amount
import domain.vo.Identity
import domain.vo.Pageable
import domain.vo.PlayerId
import kotlinx.datetime.Instant
import com.nekgamebling.game.v1.FindAllCasinoRoundQuery as FindAllCasinoRoundProto
import com.nekgamebling.game.v1.FindCasinoRoundQuery as FindCasinoRoundProto

class CasinoRoundGrpcService(
    private val bus: Bus,
) : CasinoRoundServiceGrpcKt.CasinoRoundServiceCoroutineImplBase() {

    override suspend fun find(request: FindCasinoRoundProto): FindCasinoRoundProto.Result = handleGrpcCall {
        val round = bus(FindCasinoRoundQuery(id = request.id))
            .orElseThrow { CasinoRoundNotFoundException() }

        FindCasinoRoundQueryKt.result {
            item = round.toProto()
        }
    }

    override suspend fun findAll(request: FindAllCasinoRoundProto): FindAllCasinoRoundProto.Result = handleGrpcCall {
        val page = bus(
            FindAllCasinoRoundQuery(
                playerId = if (request.hasPlayerId()) PlayerId(request.playerId) else null,
                gameIdentity = if (request.hasGameIdentity()) Identity(request.gameIdentity) else null,
                providerIdentity = if (request.hasProviderIdentity()) Identity(request.providerIdentity) else null,
                minPlaceAmount = if (request.hasMinPlaceAmount()) Amount(request.minPlaceAmount) else null,
                maxPlaceAmount = if (request.hasMaxPlaceAmount()) Amount(request.maxPlaceAmount) else null,
                minSettleAmount = if (request.hasMinSettleAmount()) Amount(request.minSettleAmount) else null,
                maxSettleAmount = if (request.hasMaxSettleAmount()) Amount(request.maxSettleAmount) else null,
                dateFrom = if (request.hasFromDate()) parseInstant("from_date", request.fromDate) else null,
                dateTo = if (request.hasToDate()) parseInstant("to_date", request.toDate) else null,
                pageable = Pageable(request.pageNum, request.pageSize),
            )
        )

        FindAllCasinoRoundQueryKt.result {
            items.addAll(page.items.map { it.toProto() })
            totalItems = page.totalItems.toInt()
            totalPages = page.totalPages.toInt()
            currentPage = page.currentPage
        }
    }
}

/**
 * A set-but-unparseable date is a caller error, not a server fault: it answers INVALID_ARGUMENT
 * instead of leaking out of [Instant.parse] as INTERNAL the way `WinnerService.FindAll` does.
 */
private fun parseInstant(field: String, value: String): Instant =
    runCatching { Instant.parse(value) }
        .getOrElse { throw InvalidDateFormatException(field, value) }
