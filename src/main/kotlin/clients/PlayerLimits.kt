package clients

import RedisConfig
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import io.lettuce.core.api.coroutines
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import org.slf4j.LoggerFactory

/** The per-player cap on a single bet, set at launch and worn down by every PLACE. */
interface PlayerLimits {

    suspend fun maxPlace(playerId: String): Long?

    suspend fun setMaxPlace(playerId: String, amount: Long)

    suspend fun decrease(playerId: String, amount: Long) {
        val current = maxPlace(playerId) ?: return
        val next = maxOf(0L, current - amount)
        setMaxPlace(playerId, next)
        log.info("Decreased player limit for [{}]: {} -> {}", playerId, current, next)
    }

    companion object {
        private val log = LoggerFactory.getLogger(PlayerLimits::class.java)
    }
}

@OptIn(ExperimentalLettuceCoroutinesApi::class)
class RedisPlayerLimits(config: RedisConfig) : PlayerLimits {

    private val client: RedisClient = RedisClient.create(
        RedisURI.builder().withHost(config.host).withPort(config.port).build(),
    )

    private val commands: RedisCoroutinesCommands<String, String> = client.connect().coroutines()

    override suspend fun maxPlace(playerId: String): Long? = commands.get(key(playerId))?.toLong()

    override suspend fun setMaxPlace(playerId: String, amount: Long) {
        commands.set(key(playerId), amount.toString())
    }

    fun close() {
        client.shutdown()
    }

    private fun key(playerId: String) = "$KEY_PREFIX$playerId"

    private companion object {
        const val KEY_PREFIX = "player:limit:max_place:"
    }
}
