package application.query.provider

import application.IQuery
import domain.model.Provider
import domain.vo.Identity
import domain.vo.Page
import domain.vo.Pageable

data class FindAllProviderQuery(
    val query: String,

    val active: Boolean? = null,

    val aggregatorId: String? = null,

    val inCollectionIdentities: List<Identity> = emptyList(),

    val pageable: Pageable,
) : IQuery<Page<Provider>>
