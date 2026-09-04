package application.query.provider

import application.IQuery
import domain.model.CasinoProvider
import domain.vo.Identity
import domain.vo.Page
import domain.vo.Pageable

data class FindAllCasinoProviderQuery(
    val query: String,

    val active: Boolean? = null,

    val inCollectionIdentities: List<Identity> = emptyList(),

    val inTags: List<String> = emptyList(),

    val pageable: Pageable,
) : IQuery<Page<CasinoProvider>>
