package infrastructure.handler.winner

import application.IQueryHandler
import application.query.winner.LastWin
import application.query.winner.LastWinnerQuery
import application.query.winner.WinnerSort
import domain.vo.Amount
import domain.vo.Currency
import domain.vo.Page
import domain.vo.PlayerId
import infrastructure.handler.game.toCondition
import infrastructure.persistence.dbRead
import infrastructure.persistence.mapper.CasinoGameMapper.toCasinoGame
import infrastructure.persistence.table.CasinoGameTable
import infrastructure.persistence.table.CasinoProviderTable
import infrastructure.persistence.table.CasinoRoundTable
import infrastructure.persistence.table.SpinTable
import org.jetbrains.exposed.sql.Expression
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.andWhere

class LastWinnerQueryHandler : IQueryHandler<LastWinnerQuery, Page<LastWin>> {

    override suspend fun handle(query: LastWinnerQuery): Page<LastWin> = dbRead {
        val baseQuery = SpinTable
            .innerJoin(CasinoRoundTable)
            .join(CasinoGameTable, JoinType.INNER, CasinoRoundTable.game, CasinoGameTable.id)
            .join(CasinoProviderTable, JoinType.INNER, CasinoGameTable.provider, CasinoProviderTable.id)
            .select(
                SpinTable.amount,
                CasinoRoundTable.createdAt,
                CasinoRoundTable.currency,
                CasinoRoundTable.playerId,
                // Список колонок обязан покрывать ВСЁ, что читают row-мапперы
                // (CasinoGameMapper.toCasinoGame -> CasinoProviderMapper.toCasinoProvider):
                // недостающая колонка роняет запрос в рантайме на первой же строке,
                // а на пустой выдаче маппер не вызывается и баг не виден.
                CasinoGameTable.identity,
                CasinoGameTable.name,
                CasinoGameTable.bonusBetEnable,
                CasinoGameTable.bonusWageringEnable,
                CasinoGameTable.tags,
                CasinoGameTable.rtp,
                CasinoGameTable.freeSpinEnable,
                CasinoGameTable.freeChipEnable,
                CasinoGameTable.jackpotEnable,
                CasinoGameTable.demoEnable,
                CasinoGameTable.bonusBuyEnable,
                CasinoGameTable.locales,
                CasinoGameTable.platforms,
                CasinoGameTable.playLines,
                CasinoGameTable.active,
                CasinoGameTable.images,
                CasinoGameTable.customImages,
                CasinoGameTable.sortOrder,
                CasinoProviderTable.identity,
                CasinoProviderTable.name,
                CasinoProviderTable.images,
                CasinoProviderTable.customImages,
                CasinoProviderTable.sortOrder,
                CasinoProviderTable.active,
                CasinoProviderTable.blockedCountry,
                CasinoProviderTable.tags,
            )
            .where {
                // A lost round settles as a zero-amount SETTLE — it is a settlement, not a win,
                // and listing it puts "0" rows in the player-facing winners feed.
                (SpinTable.type eq domain.model.SpinType.SETTLE) and
                    (SpinTable.amount greater 0L) and
                    (CasinoRoundTable.freespinId.isNull())
            }

        // Тот же предикат, что и у листингов игр: провайдер/коллекция/теги/флаги.
        query.filter?.let { filter -> baseQuery.andWhere { filter.toCondition() } }
        query.minAmount?.let { baseQuery.andWhere { SpinTable.amount greaterEq it.value } }
        query.maxAmount?.let { baseQuery.andWhere { SpinTable.amount lessEq it.value } }
        query.currency?.let { baseQuery.andWhere { CasinoRoundTable.currency eq it.value } }
        query.playerId?.let { baseQuery.andWhere { CasinoRoundTable.playerId eq it.value } }
        query.fromDate?.let { baseQuery.andWhere { CasinoRoundTable.createdAt greaterEq it } }
        query.toDate?.let { baseQuery.andWhere { CasinoRoundTable.createdAt lessEq it } }

        val totalItems = baseQuery.count()
        val pageable = query.pageable

        // Всегда по убыванию. Хвостовой ключ — id спина: createdAt и amount не
        // уникальны, а на равных ключах страницы «плывут» (строка повторяется
        // на следующей странице или пропадает).
        val ordering: Array<Pair<Expression<*>, SortOrder>> = when (query.sort) {
            WinnerSort.AMOUNT -> arrayOf(
                SpinTable.amount to SortOrder.DESC,
                SpinTable.id to SortOrder.DESC,
            )

            WinnerSort.DATE -> arrayOf(
                CasinoRoundTable.createdAt to SortOrder.DESC,
                SpinTable.id to SortOrder.DESC,
            )
        }

        val rows = baseQuery
            .orderBy(*ordering)
            .limit(pageable.sizeReal)
            .offset(pageable.offset)
            .toList()

        val items = rows.map { row ->
            LastWin(
                game = row.toCasinoGame(),
                amount = Amount(row[SpinTable.amount]),
                currency = Currency(row[CasinoRoundTable.currency]),
                playerId = PlayerId(row[CasinoRoundTable.playerId]),
                date = row[CasinoRoundTable.createdAt],
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
