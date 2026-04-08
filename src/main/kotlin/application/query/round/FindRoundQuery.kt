package application.query.round

import application.IQuery
import domain.model.Round
import domain.vo.Amount
import java.util.Optional

/**
 * Read-side view of a [Round] enriched with aggregated spin totals.
 * Reused by [FindRoundQuery] (single) and [FindAllRoundQuery] (page).
 */
data class RoundView(
    val round: Round,

    val totalPlace: Amount,

    val totalSettle: Amount,
)

data class FindRoundQuery(
    val id: Long,
) : IQuery<Optional<RoundView>>
