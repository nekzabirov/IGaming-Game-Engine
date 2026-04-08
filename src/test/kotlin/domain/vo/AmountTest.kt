package domain.vo

import domain.exception.badrequest.InvalidAmountException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class AmountTest : FunSpec({

    test("Amount.ZERO has value 0") {
        Amount.ZERO.value shouldBe 0L
    }

    test("positive amount is accepted") {
        Amount(1000).value shouldBe 1000L
    }

    test("zero amount is accepted") {
        Amount(0).value shouldBe 0L
    }

    test("negative amount throws InvalidAmountException") {
        shouldThrow<InvalidAmountException> { Amount(-1) }
    }

    test("addition sums values") {
        (Amount(100) + Amount(50)).value shouldBe 150L
    }

    test("subtraction reduces value") {
        (Amount(100) - Amount(40)).value shouldBe 60L
    }

    test("subtraction below zero throws") {
        shouldThrow<InvalidAmountException> { Amount(10) - Amount(20) }
    }

    test("comparison works across instances") {
        (Amount(100) > Amount(50)) shouldBe true
        (Amount(100) >= Amount(100)) shouldBe true
        (Amount(50) < Amount(100)) shouldBe true
    }

    test("minOf returns the smaller amount") {
        minOf(Amount(100), Amount(50)) shouldBe Amount(50)
        minOf(Amount(50), Amount(100)) shouldBe Amount(50)
    }
})
