package infrastructure.wallet

import application.port.external.IWalletPort
import com.nekgamebling.wallet.v1.Account
import com.nekgamebling.wallet.v1.DepositRequest
import com.nekgamebling.wallet.v1.GetAccountRequest
import com.nekgamebling.wallet.v1.TransactionType
import com.nekgamebling.wallet.v1.WalletServiceGrpc
import com.nekgamebling.wallet.v1.WithdrawRequest
import domain.model.PlayerBalance
import domain.vo.Amount
import domain.vo.Currency
import domain.vo.PlayerId
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WalletAdapter(
    private val config: WalletConfig
) : IWalletPort {

    private val channel: ManagedChannel by lazy {
        ManagedChannelBuilder
            .forAddress(config.address, config.port)
            .usePlaintext()
            .build()
    }

    private val stub: WalletServiceGrpc.WalletServiceBlockingStub by lazy {
        WalletServiceGrpc.newBlockingStub(channel)
    }

    override suspend fun findBalance(playerId: PlayerId, currency: Currency): PlayerBalance {
        val request = GetAccountRequest.newBuilder()
            .setPlayerId(playerId.value)
            .setCurrencyCode(currency.value)
            .build()

        val account = withContext(Dispatchers.IO) { stub.getAccount(request) }

        return account.toPlayerBalance(currency)
    }

    override suspend fun withdraw(
        playerId: PlayerId,
        transactionId: String,
        currency: Currency,
        realAmount: Amount,
        bonusAmount: Amount
    ): PlayerBalance {
        val request = WithdrawRequest.newBuilder()
            .setPlayerId(playerId.value)
            .setCurrencyCode(currency.value)
            .setRealAmount(realAmount.value)
            .setBonusAmount(bonusAmount.value)
            .setExternalId(transactionId)
            .setType(TransactionType.TRANSACTION_TYPE_SPIN)
            .build()

        val response = withContext(Dispatchers.IO) { stub.withdraw(request) }

        return response.account.toPlayerBalance(currency)
    }

    override suspend fun deposit(
        playerId: PlayerId,
        transactionId: String,
        currency: Currency,
        realAmount: Amount,
        bonusAmount: Amount
    ): PlayerBalance {
        val request = DepositRequest.newBuilder()
            .setPlayerId(playerId.value)
            .setCurrencyCode(currency.value)
            .setRealAmount(realAmount.value)
            .setBonusAmount(bonusAmount.value)
            .setExternalId(transactionId)
            .setType(TransactionType.TRANSACTION_TYPE_SPIN)
            .build()

        val response = withContext(Dispatchers.IO) { stub.deposit(request) }

        return response.account.toPlayerBalance(currency)
    }

    private fun Account.toPlayerBalance(currency: Currency): PlayerBalance = PlayerBalance(
        realAmount = Amount(realBalance),
        bonusAmount = Amount(bonusBalance),
        currency = currency,
    )
}
