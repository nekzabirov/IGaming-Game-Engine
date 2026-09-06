package services

import com.nekgamebling.game.v1.CasinoGameFilter
import com.nekgamebling.game.v1.WinnerItemDto
import com.nekgamebling.game.v1.winnerItemDto
import db.CasinoGames
import db.CasinoProviders
import db.CasinoRounds
import db.Page
import db.Pageable
import db.SpinType
import db.Spins
import dto.toCasinoGameDto
import kotlinx.datetime.Instant
import org.jetbrains.exposed.sql.Expression
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.andWhere
import plugins.dbRead

/** Ordering of the winners feed. Always descending — "latest wins" or "biggest wins". */
enum class WinnerSort { DATE, AMOUNT }

/** The player-facing winners feed: real-money SETTLE legs with a positive amount. */
class WinnerService {

    data class Query(
        val filter: CasinoGameFilter?,
        val minAmount: Long?,
        val maxAmount: Long?,
        val currency: String?,
        val playerId: String?,
        val fromDate: Instant?,
        val toDate: Instant?,
        val sort: WinnerSort,
    )

    suspend fun findAll(query: Query, pageable: Pageable): Page<WinnerItemDto> = dbRead {
        val wins = Spins
            .innerJoin(CasinoRounds)
            .join(CasinoGames, JoinType.INNER, CasinoRounds.game, CasinoGames.id)
            .join(CasinoProviders, JoinType.INNER, CasinoGames.provider, CasinoProviders.id)
            .select(
                Spins.id,
                Spins.amount,
                CasinoRounds.createdAt,
                CasinoRounds.currency,
                CasinoRounds.playerId,
                // Every column ResultRow.toCasinoGameDto reads — a missing one fails at runtime on
                // the first row, and an empty feed hides the bug.
                CasinoGames.identity,
                CasinoGames.name,
                CasinoGames.bonusBetEnable,
                CasinoGames.bonusWageringEnable,
                CasinoGames.tags,
                CasinoGames.rtp,
                CasinoGames.freeSpinEnable,
                CasinoGames.freeChipEnable,
                CasinoGames.jackpotEnable,
                CasinoGames.demoEnable,
                CasinoGames.bonusBuyEnable,
                CasinoGames.locales,
                CasinoGames.platforms,
                CasinoGames.playLines,
                CasinoGames.active,
                CasinoGames.images,
                CasinoGames.customImages,
                CasinoGames.customTags,
                CasinoGames.sortOrder,
                CasinoProviders.identity,
            )
            .where {
                // A lost round settles as a zero-amount SETTLE — a settlement, not a win.
                (Spins.type eq SpinType.SETTLE) and (Spins.amount greater 0L) and CasinoRounds.freespinId.isNull()
            }

        query.filter?.let { filter -> wins.andWhere { filter.toCondition() } }
        query.minAmount?.let { wins.andWhere { Spins.amount greaterEq it } }
        query.maxAmount?.let { wins.andWhere { Spins.amount lessEq it } }
        query.currency?.let { wins.andWhere { CasinoRounds.currency eq it } }
        query.playerId?.let { wins.andWhere { CasinoRounds.playerId eq it } }
        query.fromDate?.let { wins.andWhere { CasinoRounds.createdAt greaterEq it } }
        query.toDate?.let { wins.andWhere { CasinoRounds.createdAt lessEq it } }

        val totalItems = wins.count()

        // The spin id breaks ties: createdAt and amount are not unique, and equal keys make pages drift.
        val ordering: Array<Pair<Expression<*>, SortOrder>> = when (query.sort) {
            WinnerSort.AMOUNT -> arrayOf(Spins.amount to SortOrder.DESC, Spins.id to SortOrder.DESC)
            WinnerSort.DATE -> arrayOf(CasinoRounds.createdAt to SortOrder.DESC, Spins.id to SortOrder.DESC)
        }

        val items = wins
            .orderBy(*ordering)
            .limit(pageable.sizeReal)
            .offset(pageable.offset)
            .map { row ->
                winnerItemDto {
                    game = row.toCasinoGameDto()
                    amount = row[Spins.amount]
                    currency = row[CasinoRounds.currency]
                    playerId = row[CasinoRounds.playerId]
                    date = row[CasinoRounds.createdAt].toString()
                }
            }

        pageable.page(items, totalItems)
    }
}
