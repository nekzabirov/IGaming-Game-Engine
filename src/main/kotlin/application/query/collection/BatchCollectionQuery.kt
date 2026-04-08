package application.query.collection

import application.IQuery
import domain.model.Collection
import domain.vo.Identity

class BatchCollectionQuery(
    val identities: List<Identity>,
) : IQuery<List<Collection>>
