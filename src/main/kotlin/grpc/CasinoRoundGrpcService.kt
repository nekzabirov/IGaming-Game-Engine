package grpc

import com.nekgamebling.game.v1.CasinoRoundServiceGrpcKt
import com.nekgamebling.game.v1.FindAllCasinoRoundQuery
import com.nekgamebling.game.v1.FindAllCasinoRoundQueryKt
import com.nekgamebling.game.v1.FindCasinoRoundQuery
import com.nekgamebling.game.v1.FindCasinoRoundQueryKt
import dto.pageable
import errors.CasinoRoundNotFoundException
import errors.InvalidDateFormatException
import errors.Valid
import kotlinx.datetime.Instant
import services.RoundService

class CasinoRoundGrpcService(
    private val rounds: RoundService,
) : CasinoRoundServiceGrpcKt.CasinoRoundServiceCoroutineImplBase() {

    override suspend fun find(request: FindCasinoRoundQuery): FindCasinoRoundQuery.Result = handleGrpcCall {
        val round = rounds.find(request.id) ?: throw CasinoRoundNotFoundException()
        FindCasinoRoundQueryKt.result { item = round }
    }

    override suspend fun findAll(request: FindAllCasinoRoundQuery): FindAllCasinoRoundQuery.Result = handleGrpcCall {
        val page = rounds.findAll(
            RoundService.Query(
                playerId = if (request.hasPlayerId()) Valid.playerId(request.playerId) else null,
                gameIdentity = if (request.hasGameIdentity()) Valid.identity(request.gameIdentity) else null,
                providerIdentity = if (request.hasProviderIdentity()) Valid.identity(request.providerIdentity) else null,
                minPlaceAmount = if (request.hasMinPlaceAmount()) Valid.amount(request.minPlaceAmount) else null,
                maxPlaceAmount = if (request.hasMaxPlaceAmount()) Valid.amount(request.maxPlaceAmount) else null,
                minSettleAmount = if (request.hasMinSettleAmount()) Valid.amount(request.minSettleAmount) else null,
                maxSettleAmount = if (request.hasMaxSettleAmount()) Valid.amount(request.maxSettleAmount) else null,
                dateFrom = if (request.hasFromDate()) parseInstant("from_date", request.fromDate) else null,
                dateTo = if (request.hasToDate()) parseInstant("to_date", request.toDate) else null,
            ),
            pageable(request.pageNum, request.pageSize),
        )

        FindAllCasinoRoundQueryKt.result {
            items.addAll(page.items)
            totalItems = page.totalItems.toInt()
            totalPages = page.totalPages.toInt()
            currentPage = page.currentPage
        }
    }
}

/** A set-but-unparseable date is a caller error: INVALID_ARGUMENT, not INTERNAL. */
private fun parseInstant(field: String, value: String): Instant =
    runCatching { Instant.parse(value) }.getOrElse { throw InvalidDateFormatException(field, value) }
