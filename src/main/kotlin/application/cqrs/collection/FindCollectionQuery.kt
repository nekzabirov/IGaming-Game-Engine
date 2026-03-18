package application.cqrs.collection

import application.cqrs.IQuery
import domain.vo.Identity
import java.util.Optional

data class FindCollectionQuery(
    val identity: Identity,
) : IQuery<Optional<CollectionItem>>
