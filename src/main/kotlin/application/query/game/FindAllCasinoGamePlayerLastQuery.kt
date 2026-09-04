package application.query.game

import application.IQuery
import domain.model.CasinoGame
import domain.vo.Page
import domain.vo.Pageable
import domain.vo.PlayerId

data class FindAllCasinoGamePlayerLastQuery(
    val playerId: PlayerId,

    val pageable: Pageable,
) : IQuery<Page<CasinoGame>>
