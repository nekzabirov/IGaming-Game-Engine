package application.query.game

import application.IQuery
import domain.model.CasinoGame
import domain.vo.Identity
import java.util.Optional

data class FindCasinoGameQuery(
    val identity: Identity,
) : IQuery<Optional<CasinoGame>>
