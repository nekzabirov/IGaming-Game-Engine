package infrastructure.external

import application.port.outbound.PlayerLimitAdapter
import com.nekgamebling.application.port.outbound.storage.CacheAdapter
import kotlin.time.Duration.Companion.hours

class CachePlayerLimitAdapter(
    private val cacheAdapter: CacheAdapter
) : PlayerLimitAdapter {

    companion object {
        private val TTL = 1.hours
        private const val KEY_SUFFIX = ":spinLimitAmount"
    }

    override suspend fun saveSpinLimit(playerId: String, amount: Long) {
        cacheAdapter.save("$playerId$KEY_SUFFIX", amount, ttl = TTL)
    }

    override suspend fun deleteSpinLimit(playerId: String) {
        cacheAdapter.delete("$playerId$KEY_SUFFIX")
    }

    override suspend fun getSpinLimitAmount(playerId: String): Long? {
        return cacheAdapter.get<Long>("$playerId$KEY_SUFFIX")
    }
}
