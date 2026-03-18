package application.cqrs.session

import application.cqrs.IQuery
import domain.model.PlayerBalance

data class FindSessionBalanceQuery(val token: String) : IQuery<PlayerBalance>