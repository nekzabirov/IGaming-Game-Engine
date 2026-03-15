package com.nekgamebling.infrastructure.handler.game.query

import application.port.inbound.QueryHandler
import com.nekgamebling.application.port.inbound.game.query.FindAllGameWinsQuery
import com.nekgamebling.application.port.inbound.game.query.FindAllGameWinsResponse
import com.nekgamebling.application.port.inbound.game.query.GameWonItem
import infrastructure.persistence.exposed.table.GameTable
import infrastructure.persistence.exposed.table.GameWonTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import shared.value.Page

class FindAllGameWinsQueryHandler : QueryHandler<FindAllGameWinsQuery, FindAllGameWinsResponse> {

    override suspend fun handle(query: FindAllGameWinsQuery): Result<FindAllGameWinsResponse> = newSuspendedTransaction {
        val baseJoin = GameWonTable
            .innerJoin(GameTable, { GameWonTable.gameId }, { GameTable.id })

        val conditions = mutableListOf<Op<Boolean>>()

        query.gameIdentity?.let { conditions.add(GameTable.identity eq it) }
        query.playerId?.let { conditions.add(GameWonTable.playerId eq it) }
        query.currency?.let { conditions.add(GameWonTable.currency eq it) }
        query.minAmount?.let { conditions.add(GameWonTable.amount greaterEq it) }
        query.maxAmount?.let { conditions.add(GameWonTable.amount lessEq it) }
        query.startAt?.let { conditions.add(GameWonTable.createdAt greaterEq it) }
        query.endAt?.let { conditions.add(GameWonTable.createdAt lessEq it) }

        val whereClause = if (conditions.isNotEmpty()) {
            conditions.reduce { acc, op -> acc and op }
        } else {
            Op.TRUE
        }

        val totalItems = baseJoin
            .select(GameWonTable.id.count())
            .where { whereClause }
            .first()[GameWonTable.id.count()]

        val totalPages = query.pageable.getTotalPages(totalItems)

        val rows = baseJoin
            .selectAll()
            .where { whereClause }
            .orderBy(GameWonTable.createdAt to SortOrder.DESC)
            .limit(query.pageable.sizeReal)
            .offset(query.pageable.offset)
            .map { row ->
                GameWonItem(
                    id = row[GameWonTable.id].value.toString(),
                    gameIdentity = row[GameTable.identity],
                    playerId = row[GameWonTable.playerId],
                    amount = row[GameWonTable.amount],
                    currency = row[GameWonTable.currency],
                    createdAt = row[GameWonTable.createdAt]
                )
            }

        Result.success(
            FindAllGameWinsResponse(
                items = Page(
                    items = rows,
                    totalPages = totalPages,
                    totalItems = totalItems,
                    currentPage = query.pageable.pageReal
                )
            )
        )
    }
}
