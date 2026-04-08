package infrastructure.handler.round

import application.query.round.RoundView

import application.IQueryHandler
import application.query.round.FindRoundQuery
import domain.model.SpinType
import domain.vo.Amount
import infrastructure.persistence.entity.GameEntity
import infrastructure.persistence.entity.GameVariantEntity
import infrastructure.persistence.entity.ProviderEntity
import infrastructure.persistence.entity.RoundEntity
import infrastructure.persistence.entity.SessionEntity
import infrastructure.persistence.mapper.RoundMapper.toDomain
import infrastructure.persistence.table.SpinTable
import org.jetbrains.exposed.dao.load
import infrastructure.persistence.dbRead
import java.util.Optional

class FindRoundQueryHandler : IQueryHandler<FindRoundQuery, Optional<RoundView>> {

    override suspend fun handle(query: FindRoundQuery): Optional<RoundView> = dbRead {
        val roundEntity = RoundEntity.findById(query.id)
            ?.load(
                RoundEntity::session,
                RoundEntity::gameVariant,
                SessionEntity::gameVariant,
                GameVariantEntity::game,
                GameEntity::provider,
                GameEntity::collections,
                ProviderEntity::aggregator,
            )
            ?: return@dbRead Optional.empty()

        val totals = SpinTable
            .select(SpinTable.type, SpinTable.amount)
            .where { SpinTable.round eq query.id }
            .fold(0L to 0L) { (place, settle), row ->
                when (row[SpinTable.type]) {
                    SpinType.PLACE -> (place + row[SpinTable.amount]) to settle
                    SpinType.SETTLE -> place to (settle + row[SpinTable.amount])
                    else -> place to settle
                }
            }

        Optional.of(
            RoundView(
                round = roundEntity.toDomain(),
                totalPlace = Amount(totals.first),
                totalSettle = Amount(totals.second),
            )
        )
    }
}
