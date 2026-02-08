package com.nekgamebling.infrastructure.external

import domain.session.model.Balance
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory cache for player balances.
 * Used to avoid redundant balance fetches during rapid game operations.
 *
 * TTL: 10 seconds (balance might change from external sources)
 */
object BalanceCache {
    private data class CachedBalance(val balance: Balance, val expiresAt: Long)

    private val cache = ConcurrentHashMap<String, CachedBalance>()
    private const val TTL_MS = 10_000L // 10 seconds

    fun get(playerId: String): Balance? {
        val cached = cache[playerId] ?: return null
        if (System.currentTimeMillis() > cached.expiresAt) {
            cache.remove(playerId)
            return null
        }
        return cached.balance
    }

    fun put(playerId: String, balance: Balance) {
        cache[playerId] = CachedBalance(balance, System.currentTimeMillis() + TTL_MS)
    }

    fun update(playerId: String, realDelta: Long, bonusDelta: Long): Balance? {
        val cached = cache[playerId] ?: return null
        if (System.currentTimeMillis() > cached.expiresAt) {
            cache.remove(playerId)
            return null
        }

        val newBalance = Balance(
            real = cached.balance.real + realDelta,
            bonus = cached.balance.bonus + bonusDelta,
            currency = cached.balance.currency
        )
        put(playerId, newBalance)
        return newBalance
    }

    fun invalidate(playerId: String) {
        cache.remove(playerId)
    }
}