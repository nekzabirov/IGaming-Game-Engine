package application.cqrs.game

import application.cqrs.IQuery
import domain.model.Game
import domain.vo.Identity
import java.util.Optional

data class FindGameQuery(val identity: Identity) : IQuery<Optional<Game>>
