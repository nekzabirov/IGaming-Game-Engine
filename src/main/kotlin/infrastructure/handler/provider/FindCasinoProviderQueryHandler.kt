package infrastructure.handler.provider

import application.IQueryHandler
import application.query.provider.FindCasinoProviderQuery
import domain.model.CasinoProvider
import infrastructure.persistence.dbRead
import infrastructure.persistence.mapper.CasinoProviderMapper.toCasinoProvider
import infrastructure.persistence.table.CasinoProviderTable
import org.jetbrains.exposed.sql.selectAll
import java.util.Optional

class FindCasinoProviderQueryHandler : IQueryHandler<FindCasinoProviderQuery, Optional<CasinoProvider>> {

    override suspend fun handle(query: FindCasinoProviderQuery): Optional<CasinoProvider> = dbRead {
        Optional.ofNullable(
            CasinoProviderTable
                .selectAll()
                .where { CasinoProviderTable.identity eq query.identity.value }
                .singleOrNull()
                ?.toCasinoProvider()
        )
    }
}
