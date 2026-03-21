package application.cqrs.provider

import application.cqrs.IQuery
import domain.vo.Identity
import domain.vo.Page
import domain.vo.Pageable

data class FindAllProviderQuery(
    val query: String,

    val active: Boolean? = null,

    val aggregatorId: String? = null,

    val inTags: List<String> = emptyList(),

    val inCollectionIdentities: List<Identity> = emptyList(),

    val pageable: Pageable,
) : IQuery<Page<ProviderItem>>
