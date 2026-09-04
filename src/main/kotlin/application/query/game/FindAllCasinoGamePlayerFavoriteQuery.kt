package application.query.game

import application.IQuery
import domain.model.CasinoGame
import domain.vo.Page
import domain.vo.Pageable
import domain.vo.PlayerId

data class FindAllCasinoGamePlayerFavoriteQuery(
    val playerId: PlayerId,

    val filter: CasinoGameFilter,

    val pageable: Pageable,
) : IQuery<Page<CasinoGame>>
