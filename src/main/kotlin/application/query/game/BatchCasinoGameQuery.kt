package application.query.game

import application.IQuery
import domain.model.CasinoGame
import domain.vo.Identity

class BatchCasinoGameQuery(
    val identities: List<Identity>,
) : IQuery<List<CasinoGame>>
