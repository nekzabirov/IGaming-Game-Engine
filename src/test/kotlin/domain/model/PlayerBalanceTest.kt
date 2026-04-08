package domain.model

import domain.vo.Amount
import domain.vo.Currency
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class PlayerBalanceTest : FunSpec({

    fun balance(real: Long, bonus: Long) = PlayerBalance(
        realAmount = Amount(real),
        bonusAmount = Amount(bonus),
        currency = Currency("USD"),
    )

    test("total sums real + bonus") {
        balance(1000, 500).total shouldBe Amount(1500)
    }

    test("canAfford uses combined balance") {
        val b = balance(100, 500)
        b.canAfford(Amount(500)) shouldBe true
        b.canAfford(Amount(600)) shouldBe true
        b.canAfford(Amount(601)) shouldBe false
    }

    test("canAffordWithReal only considers real balance") {
        val b = balance(100, 500)
        b.canAffordWithReal(Amount(100)) shouldBe true
        b.canAffordWithReal(Amount(101)) shouldBe false
        b.canAffordWithReal(Amount(0)) shouldBe true
    }
})
