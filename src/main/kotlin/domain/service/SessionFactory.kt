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
import domain.vo.SessionToken

object SessionFactory {

    fun create(
        token: SessionToken,
        playerId: PlayerId,
        gameVariant: GameVariant,
        currency: Currency,
        locale: Locale,
        platform: Platform,
    ): Session {
        domainRequire(gameVariant.game.active) { GameNotActiveException() }
        domainRequire(gameVariant.game.provider.active) { ProviderNotActiveException() }
        domainRequire(gameVariant.game.provider.aggregator.active) { AggregatorNotActiveException() }

        domainRequire(gameVariant.supportsLocale(locale)) { UnsupportedLocaleException(locale) }
        domainRequire(gameVariant.supportsPlatform(platform)) { UnsupportedPlatformException(platform) }

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
