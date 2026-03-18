package application.cqrs.collection

import application.cqrs.IQuery
import domain.vo.Page
import domain.vo.Pageable

data class FindAllCollectionQuery(
    val query: String,

    val active: Boolean? = null,

    val pageable: Pageable,
) : IQuery<Page<CollectionItem>>
