package infrastructure.handler.collection

import application.IQueryHandler
import application.query.collection.FindAllCollectionQuery
import domain.model.Collection
import domain.vo.Page
import infrastructure.persistence.dbRead
import infrastructure.persistence.mapper.CollectionMapper.toCollection
import infrastructure.persistence.table.CollectionTable
import infrastructure.persistence.table.GameCollectionTable
import infrastructure.persistence.table.GameTable
import infrastructure.persistence.table.ProviderTable
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.TextColumnType
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.castTo
import org.jetbrains.exposed.sql.exists
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll

class FindAllCollectionQueryHandler : IQueryHandler<FindAllCollectionQuery, Page<Collection>> {

    override suspend fun handle(query: FindAllCollectionQuery): Page<Collection> = dbRead {
        val filterCondition = buildFilterCondition(query)
        val pageable = query.pageable

        val totalItems = CollectionTable.selectAll().where { filterCondition }.count()

        val items = CollectionTable
            .selectAll()
            .where { filterCondition }
            .orderBy(CollectionTable.sortOrder)
            .limit(pageable.sizeReal)
            .offset(pageable.offset)
            .map { it.toCollection() }

        Page(
            items = items,
            totalPages = pageable.getTotalPages(totalItems),
            totalItems = totalItems,
            currentPage = pageable.pageReal,
        )
    }

    private fun buildFilterCondition(query: FindAllCollectionQuery): Op<Boolean> {
        val conditions = buildList {
            if (query.query.isNotBlank()) {
                val pattern = "%${query.query.lowercase()}%"
                add(Op.build {
                    (CollectionTable.identity like pattern) or
                            (CollectionTable.name.castTo<String>(TextColumnType()) like pattern)
                })
            }
            query.active?.let { add(Op.build { CollectionTable.active eq it }) }

            if (query.inTags.isNotEmpty()) {
                add(exists(
                    GameCollectionTable
                        .join(GameTable, JoinType.INNER, GameCollectionTable.game, GameTable.id)
                        .select(GameCollectionTable.collection)
                        .where {
                            (GameCollectionTable.collection eq CollectionTable.id) and
                                    query.inTags.map { tag ->
                                        Op.build { GameTable.tags.castTo<String>(TextColumnType()) like "%\"$tag\"%" }
                                    }.reduce { acc, op -> acc or op }
                        }
                ))
            }

            if (query.inProviderIdentities.isNotEmpty()) {
                add(exists(
                    GameCollectionTable
                        .join(GameTable, JoinType.INNER, GameCollectionTable.game, GameTable.id)
                        .select(GameCollectionTable.collection)
                        .where {
                            (GameCollectionTable.collection eq CollectionTable.id) and
                                    (GameTable.provider inSubQuery (
                                            ProviderTable
                                                .select(ProviderTable.id)
                                                .where { ProviderTable.identity inList query.inProviderIdentities.map { it.value } }
                                            ))
                        }
                ))
            }
        }
        return conditions.reduceOrNull { acc, op -> acc and op } ?: Op.TRUE
    }
}
