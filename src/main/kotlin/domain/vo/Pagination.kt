package domain.vo

/**
 * Paginated result container.
 */
data class Page<T>(
    val items: List<T>,
    val totalPages: Long,
    val totalItems: Long = 0,
    val currentPage: Int = 1
) {
    inline fun <R> map(transform: (T) -> R) = Page(
        items = items.map(transform),
        totalPages = totalPages,
        totalItems = totalItems,
        currentPage = currentPage
    )

    companion object {
        fun <T> empty() = Page<T>(emptyList(), 0, 0, 1)
    }
}

/**
 * Pagination request parameters.
 */
data class Pageable(
    val page: Int,
    val size: Int
) {
    val pageReal: Int = page.coerceAtLeast(1)
    val sizeReal: Int = size.coerceAtLeast(1)
    val offset: Long = (pageReal - 1L) * sizeReal

    fun getTotalPages(totalItems: Long): Long =
        if (totalItems == 0L) 1L else ((totalItems + sizeReal - 1) / sizeReal)

    companion object {
        val DEFAULT = Pageable(1, 20)
    }
}
