package infrastructure.handler.provider

import application.IQueryHandler
import application.query.provider.FindAllCasinoProviderTagQuery
import domain.vo.Page
import infrastructure.persistence.dbRead
import infrastructure.persistence.table.CasinoProviderTable

class FindAllCasinoProviderTagQueryHandler : IQueryHandler<FindAllCasinoProviderTagQuery, Page<String>> {

    override suspend fun handle(query: FindAllCasinoProviderTagQuery): Page<String> = dbRead {
        // Same in-memory distinct as FindAllCasinoGameTagQueryHandler: the provider
        // catalog is tiny and the json array unnest would be dialect-specific.
        val tags = CasinoProviderTable
            .select(CasinoProviderTable.tags, CasinoProviderTable.customTags)
            .where { CasinoProviderTable.active eq true }
            .flatMap { it[CasinoProviderTable.tags] + it[CasinoProviderTable.customTags] }
            .distinct()
            .sorted()

        val pageable = query.pageable
        val fromIndex = pageable.offset.toInt().coerceAtMost(tags.size)
        val toIndex = (fromIndex + pageable.sizeReal).coerceAtMost(tags.size)

        Page(
            items = tags.subList(fromIndex, toIndex),
            totalPages = pageable.getTotalPages(tags.size.toLong()),
            totalItems = tags.size.toLong(),
            currentPage = pageable.pageReal,
        )
    }
}
