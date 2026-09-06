package services

import clients.GameHub
import clients.PlayerLimits
import db.CasinoGame
import db.CasinoGames
import db.Platform
import errors.CasinoGameNotActiveException
import errors.CasinoGameNotFoundException
import errors.CasinoProviderNotActiveException
import errors.UnsupportedPlatformException
import events.EventPublisher
import events.GamePayload
import events.SessionEvent
import events.SessionPayload
import events.toPayload
import kotlinx.datetime.Clock
import org.jetbrains.exposed.dao.with
import org.slf4j.LoggerFactory
import plugins.dbRead

/** Everything that opens a game or a grant through the hub. No local state, no session table. */
class LaunchService(
    private val gameHub: GameHub,
    private val limits: PlayerLimits,
    private val events: EventPublisher,
) {

    private val log = LoggerFactory.getLogger(LaunchService::class.java)

    private class Launchable(
        val active: Boolean,
        val providerActive: Boolean,
        val platforms: List<String>,
        val locales: List<String>,
        val payload: GamePayload,
    )

    /**
     * Fails fast, before the hub is called: an inactive game/provider or an unsupported platform
     * is cheaper to reject here than to round-trip and have the hub say NO_ROUTE. Locale is a soft
     * hint — an unsupported one falls back to English rather than refusing the launch.
     */
    suspend fun play(
        identity: String,
        playerId: String,
        locale: String,
        platform: Platform,
        currency: String,
        maxSpinPlaceAmount: Long?,
    ): String {
        val game = dbRead {
            CasinoGame.find { CasinoGames.identity eq identity }
                .with(CasinoGame::provider, CasinoGame::collections)
                .firstOrNull()
                ?.let { Launchable(it.active, it.provider.active, it.platforms, it.locales, it.toPayload()) }
        } ?: throw CasinoGameNotFoundException()

        if (maxSpinPlaceAmount != null) limits.setMaxPlace(playerId, maxSpinPlaceAmount)

        if (!game.active) throw CasinoGameNotActiveException()
        if (!game.providerActive) throw CasinoProviderNotActiveException()
        if (platform.name !in game.platforms) throw UnsupportedPlatformException(platform)
        val resolvedLocale = if (locale in game.locales) locale else DEFAULT_LOCALE

        val session = SessionPayload(
            playerId = playerId,
            game = game.payload,
            currency = currency,
            locale = resolvedLocale,
            platform = platform,
            createdAt = Clock.System.now(),
        )

        val url = gameHub.launchCasino(
            game = identity,
            playerId = playerId,
            currency = currency,
            locale = resolvedLocale,
            lobbyUrl = "",
            platform = platform,
            freespinId = null,
        )

        // The hub already accepted the launch — a broker failure past this point must not fail it.
        try {
            events.publish(SessionEvent(session))
        } catch (e: Exception) {
            log.error("EVENT PUBLISH FAILED (event lost): route={} player={}", SessionEvent.ROUTE, playerId, e)
        }

        log.info("CasinoSession opened: player={} game={}", playerId, identity)

        return url
    }

    suspend fun demo(identity: String, currency: String, locale: String, platform: Platform, lobbyUrl: String): String {
        requireGame(identity)
        return gameHub.launchCasinoDemo(identity, currency, locale, lobbyUrl, platform)
    }

    /** What the vendor lets a grant configure: its own presets plus the stake/count bounds, flattened. */
    suspend fun freespinPresets(identity: String): Map<String, Any> {
        requireGame(identity)
        val response = gameHub.freespinPresets(identity)
        return buildMap {
            putAll(response.presetsMap)
            if (response.hasMinAmount()) put("minAmount", response.minAmount)
            if (response.hasMaxAmount()) put("maxAmount", response.maxAmount)
            if (response.hasMinCount()) put("minCount", response.minCount)
            if (response.hasMaxCount()) put("maxCount", response.maxCount)
        }
    }

    /** Proxied to the hub, named by the CALLER's reference — the same one the grant's spins carry. */
    suspend fun createFreespin(
        identity: String,
        playerId: String,
        reference: String,
        currency: String,
        amount: Long,
        count: Int,
        durationSeconds: Long,
        presets: Map<String, Any>,
    ) {
        gameHub.createFreespin(
            game = identity,
            playerId = playerId,
            amount = amount,
            count = count,
            currency = currency,
            presets = presets.mapValues { it.value.toString() },
            reference = reference,
            durationSeconds = durationSeconds,
        )
    }

    suspend fun cancelFreespin(reference: String) {
        gameHub.cancelFreespin(reference)
    }

    /** No player = guest: the line is visible, no bets, no session minted anywhere. */
    suspend fun openSportbook(playerId: String?, currency: String, locale: String): Pair<String, Map<String, String>> {
        val response = gameHub.openSportbook(playerId, currency, locale)
        return response.integration to response.dataMap
    }

    private suspend fun requireGame(identity: String) {
        dbRead { CasinoGame.findByIdentity(identity) } ?: throw CasinoGameNotFoundException()
    }

    private companion object {
        const val DEFAULT_LOCALE = "en"
    }
}
