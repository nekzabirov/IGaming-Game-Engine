package application.cqrs.collection

import application.cqrs.IQuery
import domain.vo.Identity
import domain.vo.Page
import domain.vo.Pageable

data class FindAllCollectionQuery(
    val query: String,

    val active: Boolean? = null,

    val inTags: List<String> = emptyList(),

    val inProviderIdentities: List<Identity> = emptyList(),

    val pageable: Pageable,
) : IQuery<Page<CollectionItem>>
