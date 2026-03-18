package application.cqrs.round

import application.cqrs.IQuery
import java.util.Optional

data class FindRoundQuery(
    val id: Long,
) : IQuery<Optional<RoundItem>>
