package infrastructure.handler.game

import application.IQueryHandler
import application.query.game.FindCasinoGameQuery
import domain.model.CasinoGame
import infrastructure.persistence.dbRead
import infrastructure.persistence.entity.CasinoGameEntity
import infrastructure.persistence.mapper.CasinoGameMapper.toDomain
import infrastructure.persistence.table.CasinoGameTable
import org.jetbrains.exposed.dao.with
import java.util.Optional

class FindCasinoGameQueryHandler : IQueryHandler<FindCasinoGameQuery, Optional<CasinoGame>> {

    override suspend fun handle(query: FindCasinoGameQuery): Optional<CasinoGame> = dbRead {
        val entity = CasinoGameEntity.find { CasinoGameTable.identity eq query.identity.value }
            .with(CasinoGameEntity::provider, CasinoGameEntity::collections)
            .firstOrNull() ?: return@dbRead Optional.empty()

        Optional.of(entity.toDomain())
    }
}
