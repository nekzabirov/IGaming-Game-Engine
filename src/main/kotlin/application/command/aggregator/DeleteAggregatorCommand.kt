package application.command.aggregator

import application.ICommand
import domain.vo.Identity

data class DeleteAggregatorCommand(
    val identity: Identity,
) : ICommand<Unit>
