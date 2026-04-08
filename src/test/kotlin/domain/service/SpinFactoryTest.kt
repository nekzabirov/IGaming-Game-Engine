package domain.service

import domain.exception.conflict.RoundAlreadyFinishedException
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
        val (finished, _) = TestFixtures.round().finish()
        shouldThrow<RoundAlreadyFinishedException> {
            SpinFactory.place(finished, ExternalSpinId("spin_1"), Amount(100))
        }
    }

    test("settle creates a SETTLE spin") {
        val spin = SpinFactory.settle(TestFixtures.round(), ExternalSpinId("spin_2"), Amount(200))
        spin.type shouldBe SpinType.SETTLE
        spin.isSettle shouldBe true
    }

    test("settle on finished round throws") {
        val (finished, _) = TestFixtures.round().finish()
        shouldThrow<RoundAlreadyFinishedException> {
            SpinFactory.settle(finished, ExternalSpinId("spin_2"), Amount(100))
        }
    }

    test("rollback creates a ROLLBACK spin — allowed after round finish") {
        val (finished, _) = TestFixtures.round().finish()
        val spin = SpinFactory.rollback(finished, ExternalSpinId("spin_3"), Amount(100))
        spin.type shouldBe SpinType.ROLLBACK
        spin.isRollback shouldBe true
    }
})
