package domain.service

import domain.exception.badrequest.UnsupportedPlatformException
import domain.exception.conflict.CasinoGameNotActiveException
import domain.exception.conflict.CasinoProviderNotActiveException
import domain.exception.domainRequire
import domain.model.CasinoGame
import domain.model.CasinoSession
import domain.model.Platform
import domain.vo.Currency
import domain.vo.Locale
import domain.vo.PlayerId

object CasinoSessionFactory {

    private val DEFAULT_LOCALE = Locale("en")

    /**
     * Fails fast, before the hub is ever called: an inactive game/provider or an unsupported
     * platform is cheaper to reject locally than to round-trip and have the hub say `NO_ROUTE`.
     */
    fun create(
        playerId: PlayerId,
        game: CasinoGame,
        currency: Currency,
        locale: Locale,
        platform: Platform,
    ): CasinoSession {
        domainRequire(game.active) { CasinoGameNotActiveException() }
        domainRequire(game.provider.active) { CasinoProviderNotActiveException() }
        domainRequire(game.supportsPlatform(platform)) { UnsupportedPlatformException(platform) }

        // Locale is a soft UI-language hint — if the game doesn't advertise the requested one,
        // fall back to English instead of refusing to launch.
        val resolvedLocale = if (game.supportsLocale(locale)) locale else DEFAULT_LOCALE

        return CasinoSession(
            playerId = playerId,
            game = game,
            currency = currency,
            locale = resolvedLocale,
            platform = platform,
        )
    }
}
