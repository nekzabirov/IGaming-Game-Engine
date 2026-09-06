package grpc

import com.nekgamebling.game.v1.FindAllWinnersQuery
import com.nekgamebling.game.v1.FindAllWinnersQueryKt
import com.nekgamebling.game.v1.WinnerServiceGrpcKt
import com.nekgamebling.game.v1.WinnerSortDto
import dto.pageable
import errors.Valid
import kotlinx.datetime.Instant
import services.WinnerService
import services.WinnerSort

class WinnerGrpcService(
    private val winners: WinnerService,
) : WinnerServiceGrpcKt.WinnerServiceCoroutineImplBase() {

    override suspend fun findAll(request: FindAllWinnersQuery): FindAllWinnersQuery.Result = handleGrpcCall {
        val page = winners.findAll(
            WinnerService.Query(
                filter = if (request.hasFilter()) request.filter else null,
                minAmount = if (request.hasMinAmount()) Valid.amount(request.minAmount) else null,
                maxAmount = if (request.hasMaxAmount()) Valid.amount(request.maxAmount) else null,
                currency = request.currency.takeIf { it.isNotBlank() },
                playerId = request.playerId.takeIf { it.isNotBlank() },
                fromDate = request.fromDate.takeIf { it.isNotBlank() }?.let { Instant.parse(it) },
                toDate = request.toDate.takeIf { it.isNotBlank() }?.let { Instant.parse(it) },
                // UNSPECIFIED and anything unrecognised read as DATE.
                sort = if (request.sort == WinnerSortDto.WINNER_SORT_AMOUNT) WinnerSort.AMOUNT else WinnerSort.DATE,
            ),
            pageable(request.pageNum, request.pageSize),
        )

        FindAllWinnersQueryKt.result {
            items.addAll(page.items)
            totalItems = page.totalItems.toInt()
            totalPages = page.totalPages.toInt()
            currentPage = page.currentPage
        }
    }
}
