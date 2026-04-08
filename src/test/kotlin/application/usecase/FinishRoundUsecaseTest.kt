package application.usecase

import application.port.external.IEventPort
import domain.event.DomainEvent
import domain.event.RoundFinished
import domain.exception.conflict.RoundAlreadyFinishedException
import domain.repository.IRoundRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import support.TestFixtures

class FinishRoundUsecaseTest : FunSpec({

    test("successful finish saves round and publishes event") {
        val roundRepo = mockk<IRoundRepository>()
        val eventPort = mockk<IEventPort>(relaxed = true)
        val publishedSlot = slot<Iterable<DomainEvent>>()
        coEvery { eventPort.publishAll(capture(publishedSlot)) } returns Unit
        coEvery { roundRepo.save(any()) } answers { firstArg() }

        val usecase = FinishRoundUsecase(roundRepo, eventPort)
        val round = TestFixtures.round()

        val result = usecase.invoke(round)

        result.isSuccess shouldBe true
        val captured = publishedSlot.captured.toList()
        captured.size shouldBe 1
        captured.first().shouldBeInstanceOf<RoundFinished>()
        coVerify(exactly = 1) { roundRepo.save(match { it.isFinished }) }
    }

    test("finishing an already-finished round returns failure and does not publish") {
        val roundRepo = mockk<IRoundRepository>(relaxed = true)
        val eventPort = mockk<IEventPort>(relaxed = true)
        val usecase = FinishRoundUsecase(roundRepo, eventPort)

        val (finishedOnce, _) = TestFixtures.round().finish()

        val result = usecase.invoke(finishedOnce)

        result.isFailure shouldBe true
        (result.exceptionOrNull() is RoundAlreadyFinishedException) shouldBe true
        coVerify(exactly = 0) { roundRepo.save(any()) }
        coVerify(exactly = 0) { eventPort.publishAll(any()) }
    }
})
