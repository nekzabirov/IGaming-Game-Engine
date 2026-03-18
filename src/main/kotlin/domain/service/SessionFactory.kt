package domain.service

import domain.exception.badrequest.UnsupportedLocaleException
import domain.exception.badrequest.UnsupportedPlatformException
import domain.exception.conflict.AggregatorNotActiveException
import domain.exception.conflict.GameNotActiveException
import domain.exception.conflict.ProviderNotActiveException
import domain.exception.domainRequire
import domain.model.GameVariant
import domain.model.Platform
import domain.model.Session
import domain.vo.Currency
import domain.vo.Locale
import domain.vo.PlayerId

object SessionFactory {

    fun create(
        token: String,
        playerId: PlayerId,
        gameVariant: GameVariant,
        currency: Currency,
        locale: Locale,
        platform: Platform
    ): Session {
        domainRequire(gameVariant.game.active) { GameNotActiveException() }
        domainRequire(gameVariant.game.provider.active) { ProviderNotActiveException() }
        domainRequire(gameVariant.game.provider.aggregator.active) { AggregatorNotActiveException() }

        domainRequire(gameVariant.locales.contains(locale)) { UnsupportedLocaleException(locale) }
        domainRequire(gameVariant.platforms.contains(platform)) { UnsupportedPlatformException(platform) }

        return Session(
            token = token,
            gameVariant = gameVariant,
            playerId = playerId,
            externalToken = null,
            currency = currency,
            locale = locale,
            platform = platform,
        )
    }

}