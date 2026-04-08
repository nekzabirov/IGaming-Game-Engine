package domain.vo

import domain.exception.badrequest.BlankCurrencyException
import domain.exception.badrequest.BlankLocaleException
import domain.exception.badrequest.BlankPlayerIdException
import domain.exception.badrequest.BlankSessionTokenException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class CurrencyLocalePlayerIdTest : FunSpec({

    test("Currency accepts non-blank value") {
        Currency("USD").value shouldBe "USD"
    }

    test("Currency rejects blank") {
        shouldThrow<BlankCurrencyException> { Currency("") }
        shouldThrow<BlankCurrencyException> { Currency("   ") }
    }

    test("Locale accepts non-blank value") {
        Locale("en").value shouldBe "en"
    }

    test("Locale rejects blank") {
        shouldThrow<BlankLocaleException> { Locale("") }
    }

    test("PlayerId accepts non-blank value") {
        PlayerId("player-42").value shouldBe "player-42"
    }

    test("PlayerId rejects blank") {
        shouldThrow<BlankPlayerIdException> { PlayerId("") }
    }

    test("SessionToken accepts non-blank value") {
        SessionToken("tok_abc").value shouldBe "tok_abc"
    }

    test("SessionToken rejects blank") {
        shouldThrow<BlankSessionTokenException> { SessionToken("") }
    }
})
