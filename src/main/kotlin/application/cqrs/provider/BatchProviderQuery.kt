package application.cqrs.provider

import application.cqrs.IQuery
import domain.model.Provider
import domain.vo.Identity

class BatchProviderQuery(
    val identities: List<Identity>,
) : IQuery<List<Provider>>
