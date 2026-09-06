package services

import clients.Balance
import errors.InsufficientBalanceException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/** The real/bonus split is the most critical rule in the engine — exhaustive over every leg type. */
class SpinMathTest : FunSpec({

    fun balance(real: Long, bonus: Long) = Balance(real, bonus, "USD")

    test("PLACE with bonusBet off deducts entirely from real") {
        SpinMath.place(balance(1000, 500), 300, bonusBetEnabled = false) shouldBe SpinMath.Split(300, 300, 0)
    }

    test("PLACE with bonusBet off fails when real is short even if bonus would cover it") {
        shouldThrow<InsufficientBalanceException> { SpinMath.place(balance(100, 1000), 300, bonusBetEnabled = false) }
    }

    test("PLACE with bonusBet on drains real first, then bonus") {
        SpinMath.place(balance(100, 500), 300, bonusBetEnabled = true) shouldBe SpinMath.Split(300, 100, 200)
    }

    test("PLACE with bonusBet on uses only real when enough") {
        SpinMath.place(balance(1000, 500), 200, bonusBetEnabled = true) shouldBe SpinMath.Split(200, 200, 0)
    }

    test("PLACE with bonusBet on fails when the total is short") {
        shouldThrow<InsufficientBalanceException> { SpinMath.place(balance(100, 200), 5000, bonusBetEnabled = true) }
    }

    test("SETTLE of a real-only bet pays real") {
        SpinMath.settle(250, referenceBonus = 0) shouldBe SpinMath.Split(250, 250, 0)
    }

    test("SETTLE of a bonus-funded bet pays the bonus pool") {
        SpinMath.settle(250, referenceBonus = 100) shouldBe SpinMath.Split(250, 0, 250)
    }

    test("SETTLE needs no balance: an all-in cashout pays in full") {
        SpinMath.settle(3280, referenceBonus = 0) shouldBe SpinMath.Split(3280, 3280, 0)
    }

    test("ROLLBACK of a bet refunds the original split pool for pool") {
        SpinMath.refund(referenceReal = 300, referenceBonus = 200) shouldBe SpinMath.Split(500, 300, 200)
    }

    test("ROLLBACK of a win reclaims it") {
        SpinMath.reclaim(balance(1000, 500), referenceReal = 400, referenceBonus = 0) shouldBe SpinMath.Split(400, 400, 0)
    }

    test("ROLLBACK of a bonus win reclaims from the bonus pool") {
        SpinMath.reclaim(balance(1000, 500), referenceReal = 0, referenceBonus = 300) shouldBe SpinMath.Split(300, 0, 300)
    }

    test("ROLLBACK of a win is clamped when the win is already spent") {
        // A balance cannot go negative; the spin records what was actually reclaimed.
        SpinMath.reclaim(balance(200, 0), referenceReal = 900, referenceBonus = 0) shouldBe SpinMath.Split(200, 200, 0)
    }
})
