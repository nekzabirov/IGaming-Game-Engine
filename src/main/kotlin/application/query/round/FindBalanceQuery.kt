package application.query.round

import application.IQuery
import domain.model.PlayerBalance
import domain.vo.Currency
import domain.vo.PlayerId

data class FindBalanceQuery(val playerId: PlayerId, val currency: Currency) : IQuery<PlayerBalance>
