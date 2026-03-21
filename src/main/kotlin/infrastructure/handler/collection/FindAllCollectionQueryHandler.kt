package infrastructure.handler.collection

import application.cqrs.collection.CollectionItem
import application.cqrs.collection.FindAllCollectionQuery
import application.cqrs.IQueryHandler
import domain.vo.Page
import infrastructure.persistence.mapper.CollectionMapper.toCollection
import infrastructure.persistence.table.CollectionTable
import infrastructure.persistence.table.GameCollectionTable
import infrastructure.persistence.table.GameTable
import org.jetbrains.exposed.sql.Case
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.LongColumnType
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.Sum
import org.jetbrains.exposed.sql.TextColumnType
import infrastructure.persistence.table.ProviderTable
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.castTo
import org.jetbrains.exposed.sql.countDistinct
import org.jetbrains.exposed.sql.exists
import org.jetbrains.exposed.sql.longLiteral
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class FindAllCollectionQueryHandler : IQueryHandler<FindAllCollectionQuery, Page<CollectionItem>> {

    override suspend fun handle(query: FindAllCollectionQuery): Page<CollectionItem> = newSuspendedTransaction {
        val filterCondition = buildFilterCondition(query)

        val totalItems = CollectionTable.selectAll().where { filterCondition }.count()
        val pageable = query.pageable

        val activeGameCountExpr = Sum(
            Case().When(GameTable.active eq true, longLiteral(1)).Else(longLiteral(0)),
            LongColumnType()
        )
        val inactiveGameCountExpr = Sum(
            Case().When(GameTable.active eq false, longLiteral(1)).Else(longLiteral(0)),
            LongColumnType()
        )
        val providerCountExpr = GameTable.provider.countDistinct()

        val rows = CollectionTable
            .join(GameCollectionTable, JoinType.LEFT, CollectionTable.id, GameCollectionTable.collection)
            .join(GameTable, JoinType.LEFT, GameCollectionTable.game, GameTable.id)
            .select(
                CollectionTable.columns +
                        activeGameCountExpr +
                        inactiveGameCountExpr +
                        providerCountExpr
            )
            .where { filterCondition }
            .groupBy(*CollectionTable.columns.toTypedArray())
            .orderBy(CollectionTable.sortOrder)
            .limit(pageable.sizeReal)
            .offset(pageable.offset)
            .toList()

        val items = rows.map { row ->
            CollectionItem(
                collection = row.toCollection(),
                gameActiveCount = row[activeGameCountExpr] ?: 0L,
                gameDeactivateCount = row[inactiveGameCountExpr] ?: 0L,
                providerCount = row[providerCountExpr],
            )
        }

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
