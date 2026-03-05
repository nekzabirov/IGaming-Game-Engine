package infrastructure.external

import application.port.outbound.PlayerLimitAdapter
import com.nekgamebling.application.port.outbound.storage.CacheAdapter
import kotlin.time.Duration.Companion.hours

class CachePlayerLimitAdapter(
    private val cacheAdapter: CacheAdapter
) : PlayerLimitAdapter {

    companion object {
        private val TTL = 1.hours
        private const val KEY_SUFFIX = ":spinMaxAmount"
    }

    override suspend fun saveSpinMax(playerId: String, amount: Long) {
        cacheAdapter.save("$playerId$KEY_SUFFIX", amount, ttl = TTL)
    }

    override suspend fun deleteSpinMax(playerId: String) {
        cacheAdapter.delete("$playerId$KEY_SUFFIX")
    }

    override suspend fun getSpinMaxAmount(playerId: String): Long? {
        return cacheAdapter.get<Long>("$playerId$KEY_SUFFIX")
    }

    override suspend fun decreaseSpinMax(playerId: String, amount: Long) {
        val current = getSpinMaxAmount(playerId) ?: return
        val newAmount = maxOf(0L, current - amount)
        saveSpinMax(playerId, newAmount)
    }

    override suspend fun increaseSpinMax(playerId: String, amount: Long) {
        val current = getSpinMaxAmount(playerId) ?: return
        saveSpinMax(playerId, current + amount)
    }
}
