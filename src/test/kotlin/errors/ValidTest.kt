package errors

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ValidTest : FunSpec({

    test("canonical slugs are accepted as they are") {
        Valid.identity("pragmatic_play") shouldBe "pragmatic_play"
        Valid.identity("pragmatic-play") shouldBe "pragmatic-play"
    }

    test("an empty identity is its own error") {
        shouldThrow<EmptyIdentityException> { Valid.identity("") }
    }

    test("uppercase, spaces, dots and doubled or leading separators are rejected") {
        listOf("Pragmatic", "pragmatic play", "pragmatic.play", "_pragmatic", "pragmatic__play", "pragmatic-").forEach {
            shouldThrow<InvalidIdentityFormatException> { Valid.identity(it) }
        }
    }

    test("blank scalars are rejected with their own exception") {
        shouldThrow<BlankPlayerIdException> { Valid.playerId("  ") }
        shouldThrow<BlankCurrencyException> { Valid.currency("") }
        shouldThrow<BlankLocaleException> { Valid.locale("") }
        shouldThrow<BlankExternalIdException> { Valid.externalId("") }
        shouldThrow<BlankImageUrlException> { Valid.imageUrl(" ") }
    }

    test("a negative amount is rejected, zero is fine") {
        Valid.amount(0) shouldBe 0L
        shouldThrow<InvalidAmountException> { Valid.amount(-1) }
    }
})
