package application.query.game

import application.IQuery
import domain.model.CasinoGame
import domain.vo.Identity
import domain.vo.Page
import domain.vo.Pageable

data class FindAllCasinoGameCollectionQuery(
    val collection: Identity,

    val filter: CasinoGameFilter,

    val pageable: Pageable,
) : IQuery<Page<CasinoGame>>
