package application.port.external

import domain.vo.Amount
import domain.vo.PlayerId

interface IPlayerLimitPort {

    suspend fun getMaxPlaceAmount(playerId: PlayerId): Amount?

    suspend fun saveMaxPlaceAmount(playerId: PlayerId, amount: Amount)

}