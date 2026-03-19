package application.cqrs.game

import application.cqrs.IQuery
import domain.model.Game
import domain.vo.Identity

class BatchGameQuery(
    val identities: List<Identity>,
) : IQuery<List<Game>>
