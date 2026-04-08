package application.query.session

import application.IQuery
import domain.model.PlayerBalance

data class FindSessionBalanceQuery(val token: String) : IQuery<PlayerBalance>