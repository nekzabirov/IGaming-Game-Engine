package application.cqrs.aggregator

import application.cqrs.ICommand
import domain.vo.Identity

data class SaveAggregatorCommand(
    val identity: Identity,
    val config: Map<String, Any>,
    val active: Boolean,
    val integration: String,
) : ICommand<Unit>