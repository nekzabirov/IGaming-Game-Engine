package application.cqrs.aggregator

import application.cqrs.ICommand

data object SyncAllActiveAggregatorCommand : ICommand<Unit>
