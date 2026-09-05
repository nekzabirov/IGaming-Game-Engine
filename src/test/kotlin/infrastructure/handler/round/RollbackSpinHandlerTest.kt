package infrastructure.handler.round

import application.command.round.RollbackSpinCommand
import application.port.external.IEventPublisherPort
import application.port.external.IPlayerLimitPort
import application.port.external.IWalletPort
import application.usecase.ProcessSpinUsecase
import domain.model.PlayerBalance
import domain.model.Spin
import domain.repository.ISpinRepository
import domain.vo.Amount
import domain.vo.Currency
import domain.vo.PlayerId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import support.TestFixtures

/**
 * The hub walks both legs of a transaction in one rollback pass and stops at the first refusal, so
 * a leg we never saw has to look like "done" — otherwise the bet behind it is never given back.
 */
class RollbackSpinHandlerTest : FunSpec({

    val round = TestFixtures.round(currency = "UAH")
    val balance = TestFixtures.balance(real = 900, bonus = 0, currency = "UAH")

    fun handler(spinRepository: ISpinRepository, walletPort: IWalletPort) = RollbackSpinHandler(
        spinRepository = spinRepository,
        processSpinUsecase = ProcessSpinUsecase(
            spinRepository = spinRepository,
            eventPublisher = mockk<IEventPublisherPort>(relaxed = true),
            walletPort = walletPort,
            playerLimitPort = mockk<IPlayerLimitPort>(relaxed = true).also {
                coEvery { it.getMaxPlaceAmount(any()) } returns null
            },
            freespinToPayout = true,
        ),
        walletPort = walletPort,
    )

    test("a rollback of a leg we never saw answers success with no balance") {
        val spinRepository = mockk<ISpinRepository>(relaxed = true)
        coEvery { spinRepository.findByExternalId(any()) } returns null
        val walletPort = RecordingWallet(balance)

        val result = handler(spinRepository, walletPort)
            .handle(RollbackSpinCommand(externalSpinId = "leg-we-never-saw"))

        result.isSuccess shouldBe true
        // No balance: the request names only the leg, so with no leg there is no player whose
        // balance could honestly be reported.
        result.getOrThrow() shouldBe null
        walletPort.calls shouldBe emptyList()
        coVerify(exactly = 0) { spinRepository.save(any()) }
    }

    test("a rollback of a leg we know still reverses it and answers the balance") {
        val spinRepository = mockk<ISpinRepository>()
        coEvery { spinRepository.findByExternalId("leg-1") } returns
            TestFixtures.spin(round = round, externalId = "leg-1")
        coEvery { spinRepository.findByExternalId("leg-1:rollback") } returns null
        coEvery { spinRepository.save(any()) } answers { firstArg<Spin>() }
        val walletPort = RecordingWallet(balance)

        val result = handler(spinRepository, walletPort)
            .handle(RollbackSpinCommand(externalSpinId = "leg-1"))

        result.getOrThrow() shouldBe balance
        // Reversing a bet gives the money back, so the move is a deposit; the reads around it are
        // the calculator's opening balance and the handler's answer.
        walletPort.calls shouldBe listOf("findBalance", "deposit", "findBalance")
    }
})

/**
 * Hand-rolled rather than mocked: the wallet signature mixes value classes with plain params,
 * which mockk cannot build a matcher for.
 */
private class RecordingWallet(private val balance: PlayerBalance) : IWalletPort {

    val calls = mutableListOf<String>()

    override suspend fun findBalance(playerId: PlayerId, currency: Currency): PlayerBalance {
        calls += "findBalance"
        return balance
    }

    override suspend fun withdraw(
        playerId: PlayerId,
        transactionId: String,
        currency: Currency,
        realAmount: Amount,
        bonusAmount: Amount,
    ): PlayerBalance {
        calls += "withdraw"
        return balance
    }

    override suspend fun deposit(
        playerId: PlayerId,
        transactionId: String,
        currency: Currency,
        realAmount: Amount,
        bonusAmount: Amount,
    ): PlayerBalance {
        calls += "deposit"
        return balance
    }
}
