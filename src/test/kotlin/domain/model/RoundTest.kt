package domain.model

import domain.event.RoundFinished
import domain.exception.conflict.RoundAlreadyFinishedException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import support.TestFixtures

class RoundTest : FunSpec({

    test("new round is not finished") {
        TestFixtures.round().isFinished shouldBe false
    }

    test("finish() returns updated round + RoundFinished event") {
        val round = TestFixtures.round()
        val (finished, events) = round.finish()

        finished.isFinished shouldBe true
        finished.finishedAt.shouldNotBeNull()
        events.size shouldBe 1
        events.first().shouldBeInstanceOf<RoundFinished>()
    }

    test("double finish() throws RoundAlreadyFinishedException") {
        val (finished, _) = TestFixtures.round().finish()
        shouldThrow<RoundAlreadyFinishedException> { finished.finish() }
    }
})
