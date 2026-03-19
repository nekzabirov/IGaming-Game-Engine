package application.cqrs.collection

import application.cqrs.IQuery
import domain.model.Collection
import domain.vo.Identity

class BatchCollectionQuery(
    val identities: List<Identity>,
) : IQuery<List<Collection>>
