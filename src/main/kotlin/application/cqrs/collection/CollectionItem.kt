package application.cqrs.collection

import domain.model.Collection

data class CollectionItem(
    val collection: Collection,

    val gameActiveCount: Long,

    val gameDeactivateCount: Long,

    val providerCount: Long,
)