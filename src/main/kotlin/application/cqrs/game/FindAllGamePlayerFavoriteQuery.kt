package application.cqrs.game

import application.cqrs.IQuery
import domain.model.Game
import domain.vo.Page
import domain.vo.Pageable
import domain.vo.PlayerId

data class FindAllGamePlayerFavoriteQuery(
    val playerId: PlayerId,

    val pageable: Pageable,
) : IQuery<Page<Game>>
