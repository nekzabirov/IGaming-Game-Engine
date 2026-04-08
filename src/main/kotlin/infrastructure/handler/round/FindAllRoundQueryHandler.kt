package infrastructure.handler.round

import application.query.round.RoundView

import application.IQueryHandler
import application.query.round.FindAllRoundQuery
import domain.model.SpinType
import domain.vo.Amount
import domain.vo.Page
import infrastructure.persistence.entity.GameEntity
import infrastructure.persistence.entity.GameVariantEntity
import infrastructure.persistence.entity.ProviderEntity
import infrastructure.persistence.entity.RoundEntity
import infrastructure.persistence.entity.SessionEntity
import infrastructure.persistence.mapper.RoundMapper.toDomain
import infrastructure.persistence.table.GameTable
import infrastructure.persistence.table.GameVariantTable
import infrastructure.persistence.table.ProviderTable
import infrastructure.persistence.table.RoundTable
import infrastructure.persistence.table.SessionTable
import infrastructure.persistence.table.SpinTable
import org.jetbrains.exposed.dao.with
import org.jetbrains.exposed.sql.Case
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.LongColumnType
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.Sum
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.longLiteral
import infrastructure.persistence.dbRead

class FindAllRoundQueryHandler : IQueryHandler<FindAllRoundQuery, Page<RoundView>> {

    override suspend fun handle(query: FindAllRoundQuery): Page<RoundView> = dbRead {
        val placeSum = Sum(
            Case()
                .When(Op.build { SpinTable.type eq SpinType.PLACE }, SpinTable.amount)
                .Else(longLiteral(0)),
            LongColumnType()
        )

        val settleSum = Sum(
            Case()
                .When(Op.build { SpinTable.type eq SpinType.SETTLE }, SpinTable.amount)
                .Else(longLiteral(0)),
            LongColumnType()
        )

        val baseJoin = RoundTable
            .join(SpinTable, JoinType.LEFT, RoundTable.id, SpinTable.round)

        val whereConditions = buildList {
            query.playerId?.let { pid ->
                add(Op.build {
                    RoundTable.session inSubQuery (
                            SessionTable
                                .select(SessionTable.id)
                                .where { SessionTable.playerId eq pid.value }
                            )
                })
            }

            query.gameIdentity?.let { gid ->
                add(Op.build {
                    RoundTable.gameVariant inSubQuery (
                            GameVariantTable
                                .select(GameVariantTable.id)
                                .where {
                                    GameVariantTable.game inSubQuery (
                                            GameTable
                                                .select(GameTable.id)
                                                .where { GameTable.identity eq gid.value }
                                            )
                                }
                            )
                })
            }

            query.providerIdentity?.let { pid ->
                add(Op.build {
                    RoundTable.gameVariant inSubQuery (
                            GameVariantTable
                                .select(GameVariantTable.id)
                                .where {
                                    GameVariantTable.game inSubQuery (
                                            GameTable
                                                .select(GameTable.id)
                                                .where {
                                                    GameTable.provider inSubQuery (
                                                            ProviderTable
                                                                .select(ProviderTable.id)
                                                                .where { ProviderTable.identity eq pid.value }
                                                            )
                                                }
                                            )
                                }
                            )
                })
            }

            query.dateFrom?.let {
                add(Op.build { RoundTable.createdAt greaterEq it })
            }

            query.dateTo?.let {
                add(Op.build { RoundTable.createdAt lessEq it })
            }
        }

        val whereCondition = whereConditions.reduceOrNull { acc, op -> acc and op } ?: Op.TRUE

        val havingConditions = buildList {
            query.minPlaceAmount?.let {
                add(Op.build { placeSum greaterEq it.value })
            }
            query.maxPlaceAmount?.let {
                add(Op.build { placeSum lessEq it.value })
            }
            query.minSettleAmount?.let {
                add(Op.build { settleSum greaterEq it.value })
            }
            query.maxSettleAmount?.let {
                add(Op.build { settleSum lessEq it.value })
            }
        }

        val havingCondition = havingConditions.reduceOrNull { acc, op -> acc and op }

        fun buildGrouped() = baseJoin
            .select(RoundTable.id, placeSum, settleSum)
            .where { whereCondition }
            .groupBy(RoundTable.id)
            .let { q -> havingCondition?.let { q.having { it } } ?: q }

        val totalItems = buildGrouped().count()

        val pageable = query.pageable

        val pageRows = buildGrouped()
            .orderBy(RoundTable.id to SortOrder.DESC)
            .limit(pageable.sizeReal)
            .offset(pageable.offset)
            .map { row ->
                Triple(
                    row[RoundTable.id],
                    row[placeSum] ?: 0L,
                    row[settleSum] ?: 0L
                )
            }

        val roundIds = pageRows.map { it.first }
        val amountsById = pageRows.associate { it.first to (it.second to it.third) }

        val rounds = RoundEntity.forEntityIds(roundIds)
            .with(
                RoundEntity::session,
                RoundEntity::gameVariant,
                SessionEntity::gameVariant,
                GameVariantEntity::game,
                GameEntity::provider,
                GameEntity::collections,
                ProviderEntity::aggregator,
            )
            .toList()
            .associateBy { it.id }

        val items = roundIds.mapNotNull { id ->
            val entity = rounds[id] ?: return@mapNotNull null
            val (place, settle) = amountsById[id] ?: (0L to 0L)
            RoundView(
                round = entity.toDomain(),
                totalPlace = Amount(place),
                totalSettle = Amount(settle),
            )
        }

        Page(
            items = items,
            totalPages = pageable.getTotalPages(totalItems),
            totalItems = totalItems,
            currentPage = pageable.pageReal,
        )
    }
}
