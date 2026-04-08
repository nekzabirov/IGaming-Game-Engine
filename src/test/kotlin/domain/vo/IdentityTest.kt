package domain.vo

import domain.exception.badrequest.EmptyIdentityException
import domain.exception.badrequest.InvalidIdentityFormatException
import domain.exception.badrequest.InvalidIdentityGenerationException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class IdentityTest : FunSpec({

    test("valid identity is accepted") {
        Identity("pragmatic_play").value shouldBe "pragmatic_play"
    }

    test("empty identity throws") {
        shouldThrow<EmptyIdentityException> { Identity("") }
    }

    test("uppercase identity throws") {
        shouldThrow<InvalidIdentityFormatException> { Identity("Pragmatic") }
    }

    test("identity with spaces throws") {
        shouldThrow<InvalidIdentityFormatException> { Identity("pragmatic play") }
    }

    test("identity with special characters throws") {
        shouldThrow<InvalidIdentityFormatException> { Identity("pragmatic-play") }
    }

    test("Identity.generate slugifies input") {
        Identity.generate("Pragmatic Play").value shouldBe "pragmatic_play"
    }

    test("Identity.generate strips leading/trailing underscores") {
        Identity.generate("  ___Pragmatic!!!___  ").value shouldBe "pragmatic"
    }

    test("Identity.generate collapses repeated underscores") {
        Identity.generate("a   b").value shouldBe "a_b"
    }

    test("Identity.generate fails when input reduces to empty") {
        shouldThrow<InvalidIdentityGenerationException> { Identity.generate("!!!") }
    }
})
