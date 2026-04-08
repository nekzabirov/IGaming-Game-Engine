package application.query.game

import application.IQuery
import domain.model.Game
import domain.vo.Identity
import domain.vo.Page
import domain.vo.Pageable

data class FindAllGameCollectionQuery(
    val collection: Identity,

    val filter: GameFilter,

    val pageable: Pageable,
) : IQuery<Page<Game>>
