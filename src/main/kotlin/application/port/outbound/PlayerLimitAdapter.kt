package application.port.outbound

interface PlayerLimitAdapter {
    suspend fun saveSpinLimit(playerId: String, amount: Long)
    suspend fun deleteSpinLimit(playerId: String)
    suspend fun getSpinLimitAmount(playerId: String): Long?
}
