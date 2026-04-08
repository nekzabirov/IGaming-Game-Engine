package application.usecase

import application.port.external.IEventPort
import application.port.external.IGamePort
import application.port.factory.IAggregatorFactory
import domain.event.SessionOpened
import domain.repository.ISessionRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import support.TestFixtures

class OpenSessionUsecaseTest : FunSpec({

    test("happy path fetches launch URL, saves session, publishes SessionOpened") {
        val aggregatorFactory = mockk<IAggregatorFactory>()
        val gamePort = mockk<IGamePort>()
        val sessionRepo = mockk<ISessionRepository>()
        val eventPort = mockk<IEventPort>(relaxed = true)

        val session = TestFixtures.session()

        coEvery { aggregatorFactory.createGameAdapter(any()) } returns gamePort
        coEvery { gamePort.getLaunchUrl(session, "lobby") } returns "https://launch.url"
        coEvery { sessionRepo.save(session) } returns session

        val usecase = OpenSessionUsecase(aggregatorFactory, sessionRepo, eventPort)

        val result = usecase.invoke(session, "lobby")

        result.isSuccess shouldBe true
        result.getOrThrow().launchUrl shouldBe "https://launch.url"
        result.getOrThrow().session shouldBe session
        coVerify(exactly = 1) { eventPort.publish(match<SessionOpened> { it.session == session }) }
    }

    test("failing adapter propagates through runCatching as failure Result") {
        val aggregatorFactory = mockk<IAggregatorFactory>()
        val gamePort = mockk<IGamePort>()
        val sessionRepo = mockk<ISessionRepository>(relaxed = true)
        val eventPort = mockk<IEventPort>(relaxed = true)

        val session = TestFixtures.session()

        coEvery { aggregatorFactory.createGameAdapter(any()) } returns gamePort
        coEvery { gamePort.getLaunchUrl(any(), any()) } throws RuntimeException("upstream down")

        val usecase = OpenSessionUsecase(aggregatorFactory, sessionRepo, eventPort)

        val result = usecase.invoke(session, "lobby")

        result.isFailure shouldBe true
        coVerify(exactly = 0) { sessionRepo.save(any()) }
        coVerify(exactly = 0) { eventPort.publish(any<SessionOpened>()) }
    }
})
