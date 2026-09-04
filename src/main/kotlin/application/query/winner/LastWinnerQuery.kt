package application.query.winner

import application.IQuery
import application.query.game.CasinoGameFilter
import domain.model.CasinoGame
import domain.vo.Amount
import domain.vo.Currency
import domain.vo.Page
import domain.vo.Pageable
import domain.vo.PlayerId
import kotlinx.datetime.Instant

data class LastWin(
    val game: CasinoGame,
    val amount: Amount,
    val currency: Currency,
    val playerId: PlayerId,
    val date: Instant,
)

/** Чем сортируется лента победителей. Всегда по убыванию — см. WinnerSortDto. */
enum class WinnerSort { DATE, AMOUNT }

data class LastWinnerQuery(
    /** Ограничивает ленту выигрышами на играх, подходящих под фильтр каталога. */
    val filter: CasinoGameFilter? = null,

    val minAmount: Amount? = null,
    val maxAmount: Amount? = null,

    val currency: Currency? = null,

    val playerId: PlayerId? = null,

    val fromDate: Instant? = null,
    val toDate: Instant? = null,

    val sort: WinnerSort = WinnerSort.DATE,

    val pageable: Pageable
) : IQuery<Page<LastWin>>
