package application.query.provider

import application.IQuery
import domain.model.Provider
import domain.vo.Identity
import java.util.Optional

data class FindProviderQuery(
    val identity: Identity,
) : IQuery<Optional<Provider>>
