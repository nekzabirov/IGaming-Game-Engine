package application.cqrs.provider

import application.cqrs.IQuery
import domain.vo.Identity
import java.util.Optional

data class FindProviderQuery(
    val identity: Identity,
) : IQuery<Optional<ProviderItem>>
