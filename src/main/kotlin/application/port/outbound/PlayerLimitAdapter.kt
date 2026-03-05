package application.port.outbound

interface PlayerLimitAdapter {
    suspend fun saveSpinMax(playerId: String, amount: Long)
    suspend fun deleteSpinMax(playerId: String)
    suspend fun getSpinMaxAmount(playerId: String): Long?
    suspend fun decreaseSpinMax(playerId: String, amount: Long)
    suspend fun increaseSpinMax(playerId: String, amount: Long)
}
