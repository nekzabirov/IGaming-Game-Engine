package application.query.game

import application.IQuery
import domain.vo.Page
import domain.vo.Pageable
import domain.vo.PlayerId

data class FindAllGamePlayerFavoriteQuery(
    val playerId: PlayerId,

    val filter: GameFilter,

    val pageable: Pageable,
) : IQuery<Page<GameView>>
