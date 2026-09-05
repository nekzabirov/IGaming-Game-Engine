package application.usecase

import application.port.external.IEventPublisherPort
import application.port.external.IPlayerLimitPort
import application.port.external.IWalletPort
import domain.model.PlayerBalance
import domain.model.Spin
import domain.vo.Amount
import domain.vo.Currency
import domain.vo.PlayerId
import domain.repository.ISpinRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import support.TestFixtures

/**
 * The wallet move is the spin. Nothing in this service reconciles a movement that did not land, so
 * a spin whose money never moved must not be committed and must not be answered as a success.
 */
class ProcessSpinUsecaseTest : FunSpec({

    val round = TestFixtures.round(currency = "UAH")

    // A 100 bet off this balance projects to real = 900; the wallet reports something else, which
    // is what tells the answered balance apart from the calculator's guess at it.
    val currentBalance = TestFixtures.balance(real = 1000, bonus = 0, currency = "UAH")
    val walletBalance = TestFixtures.balance(real = 850, bonus = 0, currency = "UAH")

    fun usecase(
        walletPort: IWalletPort,
        spinRepository: ISpinRepository,
        eventPublisher: IEventPublisherPort,
    ) = ProcessSpinUsecase(
        spinRepository = spinRepository,
        eventPublisher = eventPublisher,
        walletPort = walletPort,
        playerLimitPort = mockk<IPlayerLimitPort>(relaxed = true).also {
            coEvery { it.getMaxPlaceAmount(any()) } returns null
        },
        freespinToPayout = true,
    )

    test("a wallet move that fails leaves the spin unwritten and the call failed") {
        val spinRepository = mockk<ISpinRepository>(relaxed = true)
        val eventPublisher = mockk<IEventPublisherPort>(relaxed = true)
        val walletPort = FakeWallet(currentBalance) { error("pam is down") }

        val result = usecase(walletPort, spinRepository, eventPublisher)
            .invoke(TestFixtures.spin(round = round))

        // Success here would confirm the leg to the hub forever, on both sides, for a bet that was
        // never taken — and no one comes back for it.
        result.isFailure shouldBe true
        coVerify(exactly = 0) { spinRepository.save(any()) }
        verify(exactly = 0) { eventPublisher.publish(any()) }
    }

    test("the balance answered is the wallet's own, not the calculator's projection") {
        val spinRepository = mockk<ISpinRepository>()
        coEvery { spinRepository.save(any()) } answers { firstArg<Spin>() }
        val walletPort = FakeWallet(currentBalance) { walletBalance }

        val response = usecase(walletPort, spinRepository, mockk(relaxed = true))
            .invoke(TestFixtures.spin(round = round))
            .getOrThrow()

        // webhook.proto: the balance in the answer is the balance AFTER the movement, which only
        // the wallet knows — anything else moving the player's money is invisible to the projection.
        response.balance shouldBe walletBalance
    }
})

/**
 * Hand-rolled rather than mocked: the wallet signature mixes value classes with plain params,
 * which mockk cannot build a matcher for.
 */
private class FakeWallet(
    private val current: PlayerBalance,
    private val onMove: () -> PlayerBalance,
) : IWalletPort {

    override suspend fun findBalance(playerId: PlayerId, currency: Currency): PlayerBalance = current

    override suspend fun withdraw(
        playerId: PlayerId,
        transactionId: String,
        currency: Currency,
        realAmount: Amount,
        bonusAmount: Amount,
    ): PlayerBalance = onMove()

    override suspend fun deposit(
        playerId: PlayerId,
        transactionId: String,
        currency: Currency,
        realAmount: Amount,
        bonusAmount: Amount,
    ): PlayerBalance = onMove()
}
