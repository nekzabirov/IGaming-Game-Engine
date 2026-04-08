package domain.model

import domain.exception.badrequest.BlankSessionTokenException
import domain.vo.ExternalRoundId
import domain.vo.FreespinId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import support.TestFixtures

class SessionTest : FunSpec({

    test("blank token is rejected") {
        shouldThrow<BlankSessionTokenException> {
            TestFixtures.session(token = "")
        }
    }

    test("openRound delegates to RoundFactory with this session") {
        val session = TestFixtures.session()
        val round = session.openRound(externalId = ExternalRoundId("rnd_42"), freespinId = null)

        round.externalId shouldBe ExternalRoundId("rnd_42")
        round.session shouldBe session
        round.freespinId shouldBe null
        round.isFinished shouldBe false
    }

    test("openRound forwards freespinId") {
        val session = TestFixtures.session()
        val round = session.openRound(externalId = ExternalRoundId("rnd_43"), freespinId = FreespinId("fs_1"))

        round.freespinId shouldBe FreespinId("fs_1")
    }
})
