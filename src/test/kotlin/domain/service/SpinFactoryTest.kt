package domain.service

import domain.exception.conflict.CasinoRoundAlreadyFinishedException
import domain.model.SpinType
import domain.vo.Amount
import domain.vo.ExternalSpinId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import support.TestFixtures

class SpinFactoryTest : FunSpec({

    test("place creates a PLACE spin") {
        val round = TestFixtures.round()
        val spin = SpinFactory.place(round, ExternalSpinId("spin_1"), Amount(100))

        spin.type shouldBe SpinType.PLACE
        spin.amount shouldBe Amount(100)
        spin.round shouldBe round
        spin.isPlace shouldBe true
    }

    test("place on finished round throws") {
        val finished = TestFixtures.round().finish()
        shouldThrow<CasinoRoundAlreadyFinishedException> {
            SpinFactory.place(finished, ExternalSpinId("spin_1"), Amount(100))
        }
    }

    test("settle creates a SETTLE spin") {
        val spin = SpinFactory.settle(TestFixtures.round(), ExternalSpinId("spin_2"), Amount(200))
        spin.type shouldBe SpinType.SETTLE
        spin.isSettle shouldBe true
    }

    test("settle on finished round throws") {
        val finished = TestFixtures.round().finish()
        shouldThrow<CasinoRoundAlreadyFinishedException> {
            SpinFactory.settle(finished, ExternalSpinId("spin_2"), Amount(100))
        }
    }

    test("rollback creates a ROLLBACK spin — allowed after round finish") {
        val finished = TestFixtures.round().finish()
        val original = TestFixtures.spin(round = finished, externalId = "spin_1", amount = Amount(100))

        val spin = SpinFactory.rollback(finished, ExternalSpinId("spin_3"), original)

        spin.type shouldBe SpinType.ROLLBACK
        spin.isRollback shouldBe true
        // The amount is taken from the reference — a rollback cannot reverse a different sum.
        spin.amount shouldBe Amount(100)
        spin.reference shouldBe original
    }

    test("выигрыш несёт ссылку на бонусную ставку своего раунда") {
        val round = TestFixtures.round()
        val bonusPlace = TestFixtures.spin(round = round, externalId = "place_1")
            .copy(bonusAmount = Amount(100))

        val settle = SpinFactory.settle(
            round = round,
            externalId = ExternalSpinId("settle_1"),
            amount = Amount(250),
            reference = bonusPlace,
        )

        // Без неё калькулятор не отличит бонусный раунд от реального и заплатит на реальный счёт:
        // бонус превращался в реальные деньги за один спин, мимо всякого отыгрыша.
        settle.reference shouldBe bonusPlace
        settle.type shouldBe SpinType.SETTLE
    }

    test("ставки бонусом не было — ссылки нет, выигрыш пойдёт на реальный") {
        val settle = SpinFactory.settle(
            round = TestFixtures.round(),
            externalId = ExternalSpinId("settle_2"),
            amount = Amount(250),
            reference = null,
        )

        settle.reference shouldBe null
    }
})