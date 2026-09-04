package api.grpc.service

import api.grpc.config.handleGrpcCall
import api.grpc.mapper.CasinoGameFilterProtoMapper.toDomain
import api.grpc.mapper.CasinoGameProtoMapper.toProto
import application.Bus
import application.query.winner.LastWinnerQuery
import application.query.winner.WinnerSort
import com.nekgamebling.game.v1.FindAllWinnersQueryKt
import com.nekgamebling.game.v1.WinnerServiceGrpcKt
import com.nekgamebling.game.v1.WinnerSortDto
import com.nekgamebling.game.v1.winnerItemDto
import domain.vo.Amount
import domain.vo.Currency
import domain.vo.Pageable
import domain.vo.PlayerId
import kotlinx.datetime.Instant
import com.nekgamebling.game.v1.FindAllWinnersQuery as FindAllWinnersProto

class WinnerGrpcService(
    private val bus: Bus,
) : WinnerServiceGrpcKt.WinnerServiceCoroutineImplBase() {

    override suspend fun findAll(request: FindAllWinnersProto): FindAllWinnersProto.Result = handleGrpcCall {
        val page = bus(
            LastWinnerQuery(
                filter = if (request.hasFilter()) request.filter.toDomain() else null,
                minAmount = if (request.hasMinAmount()) Amount(request.minAmount) else null,
                maxAmount = if (request.hasMaxAmount()) Amount(request.maxAmount) else null,
                currency = request.currency.takeIf { it.isNotBlank() }?.let { Currency(it) },
                playerId = request.playerId.takeIf { it.isNotBlank() }?.let { PlayerId(it) },
                fromDate = request.fromDate.takeIf { it.isNotBlank() }?.let { Instant.parse(it) },
                toDate = request.toDate.takeIf { it.isNotBlank() }?.let { Instant.parse(it) },
                sort = when (request.sort) {
                    WinnerSortDto.WINNER_SORT_AMOUNT -> WinnerSort.AMOUNT
                    // UNSPECIFIED и нераспознанное значение — как DATE
                    else -> WinnerSort.DATE
                },
                pageable = Pageable(request.pageNum, request.pageSize),
            )
        )

        FindAllWinnersQueryKt.result {
            items.addAll(page.items.map { win ->
                winnerItemDto {
                    game = win.game.toProto()
                    amount = win.amount.value
                    currency = win.currency.value
                    playerId = win.playerId.value
                    date = win.date.toString()
                }
            })
            totalItems = page.totalItems.toInt()
            totalPages = page.totalPages.toInt()
            currentPage = page.currentPage
        }
    }
}
