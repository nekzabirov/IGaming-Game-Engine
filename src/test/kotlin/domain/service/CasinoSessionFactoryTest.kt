package domain.service

import domain.exception.badrequest.UnsupportedPlatformException
import domain.exception.conflict.CasinoGameNotActiveException
import domain.exception.conflict.CasinoProviderNotActiveException
import domain.model.CasinoGame
import domain.model.Platform
import domain.vo.Currency
import domain.vo.Locale
import domain.vo.PlayerId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import support.TestFixtures

class CasinoSessionFactoryTest : FunSpec({

    fun call(
        game: CasinoGame = TestFixtures.game(),
        locale: String = "en",
        platform: Platform = Platform.DESKTOP,
    ) = CasinoSessionFactory.create(
        playerId = PlayerId("p"),
        game = game,
        currency = Currency("USD"),
        locale = Locale(locale),
        platform = platform,
    )

    test("happy path builds valid session") {
        val session = call()
        session.currency shouldBe Currency("USD")
    }

    test("inactive game throws") {
        val game = TestFixtures.game(active = false)
        shouldThrow<CasinoGameNotActiveException> { call(game = game) }
    }

    test("inactive provider throws") {
        val provider = TestFixtures.provider(active = false)
        val game = TestFixtures.game(provider = provider)
        shouldThrow<CasinoProviderNotActiveException> { call(game = game) }
    }

    test("unsupported locale falls back to en") {
        call(locale = "fr").locale shouldBe Locale("en")
    }

    test("unsupported platform throws") {
        shouldThrow<UnsupportedPlatformException> { call(platform = Platform.DOWNLOAD) }
    }
})
