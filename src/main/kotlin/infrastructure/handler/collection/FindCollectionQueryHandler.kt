package infrastructure.handler.collection

import application.IQueryHandler
import application.query.collection.FindCollectionQuery
import domain.model.Collection
import infrastructure.persistence.dbRead
import infrastructure.persistence.mapper.CollectionMapper.toCollection
import infrastructure.persistence.table.CollectionTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.selectAll
import java.util.Optional

class FindCollectionQueryHandler : IQueryHandler<FindCollectionQuery, Optional<Collection>> {

    override suspend fun handle(query: FindCollectionQuery): Optional<Collection> = dbRead {
        Optional.ofNullable(
            CollectionTable
                .selectAll()
                .where { CollectionTable.identity eq query.identity.value }
                .singleOrNull()
                ?.toCollection()
        )
    }
}
