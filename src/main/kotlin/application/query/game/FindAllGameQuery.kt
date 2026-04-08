package application.query.game

import application.IQuery
import domain.vo.Page
import domain.vo.Pageable

data class FindAllGameQuery(
    val filter: GameFilter,

    val pageable: Pageable,
) : IQuery<Page<GameView>>
