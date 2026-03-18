package application.cqrs.provider

import domain.model.Provider

data class ProviderItem(
    val provider: Provider,

    val gameActiveCount: Long,

    val gameDeactivateCount: Long,
)
