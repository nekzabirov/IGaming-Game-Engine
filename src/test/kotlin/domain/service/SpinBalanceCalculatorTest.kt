package domain.service

import domain.exception.badrequest.SpinReferenceRequiredException
import domain.exception.forbidden.InsufficientBalanceException
import domain.model.SpinType
import domain.vo.Amount
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import support.TestFixtures

/**
 * The spin balance calculator is the most critical domain rule in the codebase —
 * exhaustive coverage across PLACE / SETTLE / ROLLBACK with both bonusBet on/off.
 */
class SpinBalanceCalculatorTest : FunSpec({

    val bonusGame = TestFixtures.game(bonusBetEnable = true)
    val realOnlyGame = TestFixtures.game(bonusBetEnable = false)

    fun placeSpin(bonusEnabled: Boolean, amount: Long) = TestFixtures.spin(
        round = TestFixtures.round(
            session = TestFixtures.session(
                variant = TestFixtures.gameVariant(game = if (bonusEnabled) bonusGame else realOnlyGame)
            )
        ),
        type = SpinType.PLACE,
        amount = Amount(amount),
    )

    test("PLACE with bonusBet off deducts entirely from real balance") {
        val spin = placeSpin(bonusEnabled = false, amount = 300)
        val balance = TestFixtures.balance(real = 1000, bonus = 500)

        val result = SpinBalanceCalculator.process(balance, spin)

        result.balance.realAmount shouldBe Amount(700)
        result.balance.bonusAmount shouldBe Amount(500)
        result.spin.realAmount shouldBe Amount(300)
        result.spin.bonusAmount shouldBe Amount.ZERO
    }

    test("PLACE with bonusBet off fails when real balance insufficient even if bonus would cover it") {
        val spin = placeSpin(bonusEnabled = false, amount = 300)
        val balance = TestFixtures.balance(real = 100, bonus = 1000)

        shouldThrow<InsufficientBalanceException> { SpinBalanceCalculator.process(balance, spin) }
    }

    test("PLACE with bonusBet on drains real first, then bonus") {
        val spin = placeSpin(bonusEnabled = true, amount = 300)
        val balance = TestFixtures.balance(real = 100, bonus = 500)

        val result = SpinBalanceCalculator.process(balance, spin)

        result.balance.realAmount shouldBe Amount.ZERO
        result.balance.bonusAmount shouldBe Amount(300)
        result.spin.realAmount shouldBe Amount(100)
        result.spin.bonusAmount shouldBe Amount(200)
    }

    test("PLACE with bonusBet on uses only real when enough") {
        val spin = placeSpin(bonusEnabled = true, amount = 200)
        val balance = TestFixtures.balance(real = 1000, bonus = 500)

        val result = SpinBalanceCalculator.process(balance, spin)

        result.balance.realAmount shouldBe Amount(800)
        result.balance.bonusAmount shouldBe Amount(500)
        result.spin.realAmount shouldBe Amount(200)
        result.spin.bonusAmount shouldBe Amount.ZERO
    }

    test("PLACE with bonusBet on fails when total balance insufficient") {
        val spin = placeSpin(bonusEnabled = true, amount = 5000)
        val balance = TestFixtures.balance(real = 100, bonus = 200)

        shouldThrow<InsufficientBalanceException> { SpinBalanceCalculator.process(balance, spin) }
    }

    test("SETTLE of real-only spin deposits into real balance") {
        val placedRef = TestFixtures.spin(
            type = SpinType.PLACE,
            amount = Amount(100),
        ).copy(realAmount = Amount(100), bonusAmount = Amount.ZERO)

        val settle = TestFixtures.spin(
            type = SpinType.SETTLE,
            externalId = "spin_2",
            amount = Amount(250),
            reference = placedRef,
        )
        val balance = TestFixtures.balance(real = 500, bonus = 500)

        val result = SpinBalanceCalculator.process(balance, settle)

        result.balance.realAmount shouldBe Amount(750)
        result.balance.bonusAmount shouldBe Amount(500)
        result.spin.realAmount shouldBe Amount(250)
        result.spin.bonusAmount shouldBe Amount.ZERO
    }

    test("SETTLE of bonus-funded spin deposits into bonus balance") {
        val placedRef = TestFixtures.spin(
            type = SpinType.PLACE,
            amount = Amount(100),
        ).copy(realAmount = Amount.ZERO, bonusAmount = Amount(100))

        val settle = TestFixtures.spin(
            type = SpinType.SETTLE,
            externalId = "spin_2",
            amount = Amount(250),
            reference = placedRef,
        )
        val balance = TestFixtures.balance(real = 500, bonus = 500)

        val result = SpinBalanceCalculator.process(balance, settle)

        result.balance.realAmount shouldBe Amount(500)
        result.balance.bonusAmount shouldBe Amount(750)
        result.spin.bonusAmount shouldBe Amount(250)
        result.spin.realAmount shouldBe Amount.ZERO
    }

    test("ROLLBACK restores original real + bonus breakdown from reference spin") {
        val placedRef = TestFixtures.spin(
            type = SpinType.PLACE,
            amount = Amount(500),
        ).copy(realAmount = Amount(300), bonusAmount = Amount(200))

        val rollback = TestFixtures.spin(
            type = SpinType.ROLLBACK,
            externalId = "spin_3",
            amount = Amount(500),
            reference = placedRef,
        )
        // Balance must pass the calculator's canAfford pre-check.
        val balance = TestFixtures.balance(real = 1000, bonus = 500)

        val result = SpinBalanceCalculator.process(balance, rollback)

        result.balance.realAmount shouldBe Amount(1300)
        result.balance.bonusAmount shouldBe Amount(700)
        result.spin.realAmount shouldBe Amount(300)
        result.spin.bonusAmount shouldBe Amount(200)
        result.spin.amount shouldBe Amount(500)
    }

    test("ROLLBACK without reference throws") {
        val rollback = TestFixtures.spin(
            type = SpinType.ROLLBACK,
            externalId = "spin_4",
            amount = Amount(100),
            reference = null,
        )
        val balance = TestFixtures.balance(real = 1000, bonus = 500)

        shouldThrow<SpinReferenceRequiredException> { SpinBalanceCalculator.process(balance, rollback) }
    }
})
