package infrastructure.handler.round

import application.cqrs.IQueryHandler
import application.cqrs.round.FindRoundQuery
import application.cqrs.round.RoundItem
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
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.Optional

class FindRoundQueryHandler : IQueryHandler<FindRoundQuery, Optional<RoundItem>> {

    override suspend fun handle(query: FindRoundQuery): Optional<RoundItem> = newSuspendedTransaction {
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
            ?: return@newSuspendedTransaction Optional.empty()

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
            RoundItem(
                round = roundEntity.toDomain(),
                totalPlace = Amount(totals.first),
                totalSettle = Amount(totals.second),
            )
        )
    }
}
