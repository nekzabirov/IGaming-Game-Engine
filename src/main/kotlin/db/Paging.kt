package db

data class Page<T>(
    val items: List<T>,
    val totalPages: Long,
    val totalItems: Long = 0,
    val currentPage: Int = 1,
) {
    inline fun <R> map(transform: (T) -> R): Page<R> = Page(
        items = items.map(transform),
        totalPages = totalPages,
        totalItems = totalItems,
        currentPage = currentPage,
    )
}

/** 1-based, engine-wide: page 0 and page 1 are the same first page, size 0 reads one row. */
data class Pageable(
    val page: Int,
    val size: Int,
) {
    val pageReal: Int = page.coerceAtLeast(1)
    val sizeReal: Int = size.coerceAtLeast(1)
    val offset: Long = (pageReal - 1L) * sizeReal

    fun totalPages(totalItems: Long): Long =
        if (totalItems == 0L) 1L else (totalItems + sizeReal - 1) / sizeReal

    fun <T> page(items: List<T>, totalItems: Long): Page<T> = Page(
        items = items,
        totalPages = totalPages(totalItems),
        totalItems = totalItems,
        currentPage = pageReal,
    )
}
