package infrastructure.redis

import application.port.external.IPlayerLimitPort
import domain.vo.Amount
import domain.vo.PlayerId
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import io.lettuce.core.api.coroutines
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands

@OptIn(ExperimentalLettuceCoroutinesApi::class)
class PlayerLimitRedis(
    config: RedisConfig
) : IPlayerLimitPort {

    private companion object {
        const val KEY_PREFIX = "player:limit:max_place:"
    }

    private val commands: RedisCoroutinesCommands<String, String> = RedisClient
        .create(RedisURI.builder().withHost(config.host).withPort(config.port).build())
        .connect()
        .coroutines()

    override suspend fun getMaxPlaceAmount(playerId: PlayerId): Amount? {
        val value = commands.get("$KEY_PREFIX${playerId.value}") ?: return null
        return Amount(value.toLong())
    }

    override suspend fun saveMaxPlaceAmount(playerId: PlayerId, amount: Amount) {
        commands.set("$KEY_PREFIX${playerId.value}", amount.value.toString())
    }
}
