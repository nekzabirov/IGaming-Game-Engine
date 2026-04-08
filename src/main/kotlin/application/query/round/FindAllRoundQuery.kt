package application.query.round

import application.IQuery
import domain.vo.Amount
import domain.vo.Identity
import domain.vo.Page
import domain.vo.Pageable
import domain.vo.PlayerId
import kotlinx.datetime.LocalDateTime

data class FindAllRoundQuery(
    val playerId: PlayerId?,

    val gameIdentity: Identity?,

    val providerIdentity: Identity?,

    val minPlaceAmount: Amount?,

    val maxPlaceAmount: Amount?,

    val minSettleAmount: Amount?,

    val maxSettleAmount: Amount?,

    val dateFrom: LocalDateTime?,

    val dateTo: LocalDateTime?,

    val pageable: Pageable,
) : IQuery<Page<RoundView>>
