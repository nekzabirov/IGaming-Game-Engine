package services

import com.nekgamebling.game.v1.CasinoRoundViewDto
import com.nekgamebling.game.v1.casinoRoundViewDto
import db.CasinoGame
import db.CasinoGames
import db.CasinoProviders
import db.CasinoRound
import db.CasinoRounds
import db.Page
import db.Pageable
import db.SpinType
import db.Spins
import dto.toDto
import kotlinx.datetime.Instant
import org.jetbrains.exposed.dao.load
import org.jetbrains.exposed.dao.with
import org.jetbrains.exposed.sql.Case
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.LongColumnType
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.Sum
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.longLiteral
import plugins.dbRead

/** Round history, read-only: the ledger is written by the hub's money path ([WalletService]). */
class RoundService {

    data class Query(
        val playerId: String?,
        val gameIdentity: String?,
        val providerIdentity: String?,
        val minPlaceAmount: Long?,
        val maxPlaceAmount: Long?,
        val minSettleAmount: Long?,
        val maxSettleAmount: Long?,
        val dateFrom: Instant?,
        val dateTo: Instant?,
    )

    suspend fun find(id: Long): CasinoRoundViewDto? = dbRead {
        val round = CasinoRound.findById(id)
            ?.load(CasinoRound::game, CasinoGame::provider, CasinoGame::collections)
            ?: return@dbRead null

        val totals = Spins
            .select(Spins.type, Spins.amount)
            .where { Spins.round eq id }
            .fold(0L to 0L) { (place, settle), row ->
                when (row[Spins.type]) {
                    SpinType.PLACE -> (place + row[Spins.amount]) to settle
                    SpinType.SETTLE -> place to (settle + row[Spins.amount])
                    SpinType.ROLLBACK -> place to settle
                }
            }

        casinoRoundViewDto {
            this.round = round.toDto()
            totalPlace = totals.first
            totalSettle = totals.second
        }
    }

    /** Newest round first (id DESC); amount bounds apply to the round's PLACE / SETTLE totals. */
    suspend fun findAll(query: Query, pageable: Pageable): Page<CasinoRoundViewDto> = dbRead {
        val placeSum = Sum(Case().When(Op.build { Spins.type eq SpinType.PLACE }, Spins.amount).Else(longLiteral(0)), LongColumnType())
        val settleSum = Sum(Case().When(Op.build { Spins.type eq SpinType.SETTLE }, Spins.amount).Else(longLiteral(0)), LongColumnType())

        val where = buildList {
            query.playerId?.let { add(Op.build { CasinoRounds.playerId eq it }) }
            query.gameIdentity?.let { identity ->
                add(Op.build {
                    CasinoRounds.game inSubQuery CasinoGames.select(CasinoGames.id).where { CasinoGames.identity eq identity }
                })
            }
            query.providerIdentity?.let { identity ->
                add(Op.build {
                    CasinoRounds.game inSubQuery CasinoGames.select(CasinoGames.id).where {
                        CasinoGames.provider inSubQuery CasinoProviders.select(CasinoProviders.id)
                            .where { CasinoProviders.identity eq identity }
                    }
                })
            }
            query.dateFrom?.let { add(Op.build { CasinoRounds.createdAt greaterEq it }) }
            query.dateTo?.let { add(Op.build { CasinoRounds.createdAt lessEq it }) }
        }.reduceOrNull { acc, op -> acc and op } ?: Op.TRUE

        val having = buildList {
            query.minPlaceAmount?.let { add(Op.build { placeSum greaterEq it }) }
            query.maxPlaceAmount?.let { add(Op.build { placeSum lessEq it }) }
            query.minSettleAmount?.let { add(Op.build { settleSum greaterEq it }) }
            query.maxSettleAmount?.let { add(Op.build { settleSum lessEq it }) }
        }.reduceOrNull { acc, op -> acc and op }

        fun grouped() = CasinoRounds
            .join(Spins, JoinType.LEFT, CasinoRounds.id, Spins.round)
            .select(CasinoRounds.id, placeSum, settleSum)
            .where { where }
            .groupBy(CasinoRounds.id)
            .let { q -> having?.let { q.having { it } } ?: q }

        val totalItems = grouped().count()

        val rows = grouped()
            .orderBy(CasinoRounds.id to SortOrder.DESC)
            .limit(pageable.sizeReal)
            .offset(pageable.offset)
            .map { row -> Triple(row[CasinoRounds.id], row[placeSum] ?: 0L, row[settleSum] ?: 0L) }

        val rounds = CasinoRound.forEntityIds(rows.map { it.first })
            .with(CasinoRound::game, CasinoGame::provider, CasinoGame::collections)
            .associateBy { it.id }

        val items = rows.mapNotNull { (id, place, settle) ->
            val round = rounds[id] ?: return@mapNotNull null
            casinoRoundViewDto {
                this.round = round.toDto()
                totalPlace = place
                totalSettle = settle
            }
        }

        pageable.page(items, totalItems)
    }
}
