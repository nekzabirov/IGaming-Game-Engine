package domain.service

import domain.exception.badrequest.UnsupportedLocaleException
import domain.exception.badrequest.UnsupportedPlatformException
import domain.exception.conflict.AggregatorNotActiveException
import domain.exception.conflict.GameNotActiveException
import domain.exception.conflict.ProviderNotActiveException
import domain.model.Platform
import domain.vo.Currency
import domain.vo.Locale
import domain.vo.PlayerId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import domain.vo.SessionToken
import support.TestFixtures

class SessionFactoryTest : FunSpec({

    fun call(
        game: domain.model.Game = TestFixtures.game(),
        locale: String = "en",
        platform: Platform = Platform.DESKTOP,
    ) = SessionFactory.create(
        token = SessionToken("t"),
        playerId = PlayerId("p"),
        gameVariant = TestFixtures.gameVariant(
            game = game,
            locales = listOf(Locale("en")),
            platforms = listOf(Platform.DESKTOP, Platform.MOBILE),
        ),
        currency = Currency("USD"),
        locale = Locale(locale),
        platform = platform,
    )

    test("happy path builds valid session") {
        val session = call()
        session.token shouldBe SessionToken("t")
        session.currency shouldBe Currency("USD")
    }

    test("inactive game throws") {
        val game = TestFixtures.game(active = false)
        shouldThrow<GameNotActiveException> { call(game = game) }
    }

    test("inactive provider throws") {
        val provider = TestFixtures.provider(active = false)
        val game = TestFixtures.game(provider = provider)
        shouldThrow<ProviderNotActiveException> { call(game = game) }
    }

    test("inactive aggregator throws") {
        val aggregator = TestFixtures.aggregator(active = false)
        val provider = TestFixtures.provider(aggregator = aggregator)
        val game = TestFixtures.game(provider = provider)
        shouldThrow<AggregatorNotActiveException> { call(game = game) }
    }

    test("unsupported locale throws") {
        shouldThrow<UnsupportedLocaleException> { call(locale = "fr") }
    }

    test("unsupported platform throws") {
        shouldThrow<UnsupportedPlatformException> { call(platform = Platform.DOWNLOAD) }
    }
})
