package infrastructure.handler.round

import application.query.round.CasinoRoundView

import application.IQueryHandler
import application.query.round.FindAllCasinoRoundQuery
import domain.model.SpinType
import domain.vo.Amount
import domain.vo.Page
import infrastructure.persistence.entity.CasinoGameEntity
import infrastructure.persistence.entity.CasinoRoundEntity
import infrastructure.persistence.mapper.CasinoRoundMapper.toDomain
import infrastructure.persistence.table.CasinoGameTable
import infrastructure.persistence.table.CasinoProviderTable
import infrastructure.persistence.table.CasinoRoundTable
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

class FindAllCasinoRoundQueryHandler : IQueryHandler<FindAllCasinoRoundQuery, Page<CasinoRoundView>> {

    override suspend fun handle(query: FindAllCasinoRoundQuery): Page<CasinoRoundView> = dbRead {
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

        val baseJoin = CasinoRoundTable
            .join(SpinTable, JoinType.LEFT, CasinoRoundTable.id, SpinTable.round)

        val whereConditions = buildList {
            query.playerId?.let { pid ->
                add(Op.build { CasinoRoundTable.playerId eq pid.value })
            }

            query.gameIdentity?.let { gid ->
                add(Op.build {
                    CasinoRoundTable.game inSubQuery (
                        CasinoGameTable
                            .select(CasinoGameTable.id)
                            .where { CasinoGameTable.identity eq gid.value }
                    )
                })
            }

            query.providerIdentity?.let { pid ->
                add(Op.build {
                    CasinoRoundTable.game inSubQuery (
                        CasinoGameTable
                            .select(CasinoGameTable.id)
                            .where {
                                CasinoGameTable.provider inSubQuery (
                                    CasinoProviderTable
                                        .select(CasinoProviderTable.id)
                                        .where { CasinoProviderTable.identity eq pid.value }
                                )
                            }
                    )
                })
            }

            query.dateFrom?.let {
                add(Op.build { CasinoRoundTable.createdAt greaterEq it })
            }

            query.dateTo?.let {
                add(Op.build { CasinoRoundTable.createdAt lessEq it })
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
            .select(CasinoRoundTable.id, placeSum, settleSum)
            .where { whereCondition }
            .groupBy(CasinoRoundTable.id)
            .let { q -> havingCondition?.let { q.having { it } } ?: q }

        val totalItems = buildGrouped().count()

        val pageable = query.pageable

        val pageRows = buildGrouped()
            .orderBy(CasinoRoundTable.id to SortOrder.DESC)
            .limit(pageable.sizeReal)
            .offset(pageable.offset)
            .map { row ->
                Triple(
                    row[CasinoRoundTable.id],
                    row[placeSum] ?: 0L,
                    row[settleSum] ?: 0L
                )
            }

        val roundIds = pageRows.map { it.first }
        val amountsById = pageRows.associate { it.first to (it.second to it.third) }

        val rounds = CasinoRoundEntity.forEntityIds(roundIds)
            .with(
                CasinoRoundEntity::game,
                CasinoGameEntity::provider,
                CasinoGameEntity::collections,
            )
            .toList()
            .associateBy { it.id }

        val items = roundIds.mapNotNull { id ->
            val entity = rounds[id] ?: return@mapNotNull null
            val (place, settle) = amountsById[id] ?: (0L to 0L)
            CasinoRoundView(
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
