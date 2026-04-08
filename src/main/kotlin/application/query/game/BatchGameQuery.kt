package application.query.game

import application.IQuery
import domain.vo.Identity

class BatchGameQuery(
    val identities: List<Identity>,
) : IQuery<List<GameView>>
