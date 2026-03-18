package infrastructure.handler.game

import application.cqrs.IQueryHandler
import application.cqrs.game.FindAllGameQuery
import domain.model.Game
import domain.vo.Page
import infrastructure.persistence.entity.GameEntity
import infrastructure.persistence.entity.GameVariantEntity
import infrastructure.persistence.entity.ProviderEntity
import infrastructure.persistence.mapper.GameMapper.toDomain
import infrastructure.persistence.mapper.GameVariantMapper
import infrastructure.persistence.table.CollectionTable
import infrastructure.persistence.table.GameCollectionTable
import infrastructure.persistence.table.GameTable
import infrastructure.persistence.table.GameVariantTable
import infrastructure.persistence.table.ProviderTable
import org.jetbrains.exposed.dao.with
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.TextColumnType
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.castTo
import org.jetbrains.exposed.sql.exists
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class FindAllGameQueryHandler : IQueryHandler<FindAllGameQuery, Page<Game>> {

    override suspend fun handle(query: FindAllGameQuery): Page<Game> = newSuspendedTransaction {
        val conditions = buildList {
            if (query.query.isNotBlank()) {
                val pattern = "%${query.query.lowercase()}%"
                add(Op.build {
                    (GameTable.identity like pattern) or (GameTable.name like pattern)
                })
            }

            query.active?.let {
                add(Op.build { GameTable.active eq it })
            }

            query.bonusBetEnable?.let {
                add(Op.build { GameTable.bonusBetEnable eq it })
            }

            query.bonusWageringEnabled?.let {
                add(Op.build { GameTable.bonusWageringEnable eq it })
            }

            if (query.inProviderIdentities.isNotEmpty()) {
                add(Op.build {
                    GameTable.provider inSubQuery (
                        ProviderTable
                            .select(ProviderTable.id)
                            .where { ProviderTable.identity inList query.inProviderIdentities.map { it.value } }
                    )
                })
            }

            if (query.inCollectionIdentities.isNotEmpty()) {
                add(exists(
                    GameCollectionTable
                        .select(GameCollectionTable.game)
                        .where {
                            (GameCollectionTable.game eq GameTable.id) and
                                    (GameCollectionTable.collection inSubQuery (
                                        CollectionTable
                                            .select(CollectionTable.id)
                                            .where { CollectionTable.identity inList query.inCollectionIdentities.map { it.value } }
                                    ))
                        }
                ))
            }

            val variantConditions = buildList {
                query.freeSpinEnable?.let {
                    add(Op.build { GameVariantTable.freeSpinEnable eq it })
                }
                query.freeChipEnable?.let {
                    add(Op.build { GameVariantTable.freeChipEnable eq it })
                }
                query.jackpotEnable?.let {
                    add(Op.build { GameVariantTable.jackpotEnable eq it })
                }
                query.demoEnable?.let {
                    add(Op.build { GameVariantTable.demoEnable eq it })
                }
                query.bonusBuyEnable?.let {
                    add(Op.build { GameVariantTable.bonusBuyEnable eq it })
                }
            }

            if (variantConditions.isNotEmpty()) {
                val variantCondition = variantConditions.reduce { acc, op -> acc and op }
                add(exists(
                    GameVariantTable
                        .select(GameVariantTable.id)
                        .where {
                            (GameVariantTable.game eq GameTable.id) and variantCondition
                        }
                ))
            }

            if (query.inTags.isNotEmpty()) {
                add(query.inTags.map { tag ->
                    Op.build { GameTable.tags.castTo<String>(TextColumnType()) like "%\"$tag\"%" }
                }.reduce { acc, op -> acc or op })
            }
        }

        val condition = conditions.reduceOrNull { acc, op -> acc and op } ?: Op.TRUE

        val baseQuery = GameEntity.find { condition }

        val totalItems = baseQuery.count()

        val pageable = query.pageable

        val entities = baseQuery
            .orderBy(GameTable.sortOrder to SortOrder.ASC)
            .limit(pageable.sizeReal)
            .offset(pageable.offset)
            .with(GameEntity::provider, GameEntity::collections, ProviderEntity::aggregator)
            .toList()

        val gameIds = entities.map { it.id }
        val integrations = entities.map { it.provider.aggregator.integration }.distinct()

        val variants = GameVariantEntity.find {
            (GameVariantTable.game inList gameIds) and
                    (GameVariantTable.integration inList integrations)
        }.toList()

        val variantMap = variants.associateBy { it.game.id.value to it.integration }

        val items = entities.map { entity ->
            val game = entity.toDomain()
            val variantEntity = variantMap[entity.id.value to entity.provider.aggregator.integration]
            if (variantEntity != null) {
                game.variant = GameVariantMapper.run { variantEntity.toDomain() }
            }
            game
        }

        Page(
            items = items,
            totalPages = pageable.getTotalPages(totalItems),
            totalItems = totalItems,
            currentPage = pageable.pageReal,
        )
    }
}
