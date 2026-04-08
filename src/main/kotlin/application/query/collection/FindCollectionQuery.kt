package application.query.collection

import application.IQuery
import domain.model.Collection
import domain.vo.Identity
import java.util.Optional

data class FindCollectionQuery(
    val identity: Identity,
) : IQuery<Optional<Collection>>
