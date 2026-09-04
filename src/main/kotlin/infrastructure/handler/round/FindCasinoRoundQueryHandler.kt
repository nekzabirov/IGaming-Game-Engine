package infrastructure.handler.round

import application.query.round.CasinoRoundView

import application.IQueryHandler
import application.query.round.FindCasinoRoundQuery
import domain.model.SpinType
import domain.vo.Amount
import infrastructure.persistence.entity.CasinoGameEntity
import infrastructure.persistence.entity.CasinoRoundEntity
import infrastructure.persistence.mapper.CasinoRoundMapper.toDomain
import infrastructure.persistence.table.SpinTable
import org.jetbrains.exposed.dao.load
import infrastructure.persistence.dbRead
import java.util.Optional

class FindCasinoRoundQueryHandler : IQueryHandler<FindCasinoRoundQuery, Optional<CasinoRoundView>> {

    override suspend fun handle(query: FindCasinoRoundQuery): Optional<CasinoRoundView> = dbRead {
        val roundEntity = CasinoRoundEntity.findById(query.id)
            ?.load(
                CasinoRoundEntity::game,
                CasinoGameEntity::provider,
                CasinoGameEntity::collections,
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
            CasinoRoundView(
                round = roundEntity.toDomain(),
                totalPlace = Amount(totals.first),
                totalSettle = Amount(totals.second),
            )
        )
    }
}
