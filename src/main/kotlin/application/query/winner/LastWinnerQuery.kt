package application.query.winner

import application.IQuery
import domain.model.Game
import domain.model.GameVariant
import domain.vo.Amount
import domain.vo.Currency
import domain.vo.Identity
import domain.vo.Page
import domain.vo.Pageable
import domain.vo.PlayerId
import kotlinx.datetime.LocalDateTime

data class LastWin(
    val game: Game,
    val variant: GameVariant?,
    val amount: Amount,
    val currency: Currency,
    val playerId: PlayerId,
    val date: LocalDateTime,
)

data class LastWinnerQuery(
    val gameIdentity: Identity? = null,

    val minAmount: Amount? = null,
    val maxAmount: Amount? = null,

    val currency: Currency? = null,

    val playerId: PlayerId? = null,

    val fromDate: LocalDateTime? = null,
    val toDate: LocalDateTime? = null,

    val pageable: Pageable
) : IQuery<Page<LastWin>>
