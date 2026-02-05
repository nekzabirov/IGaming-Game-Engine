package infrastructure.external.walletEngine

import application.port.outbound.external.WalletAdapter
import com.nekgamebling.wallet.*
import domain.common.value.SpinType
import domain.session.model.Balance
import infrastructure.external.turbo.BalanceCache
import infrastructure.persistence.exposed.table.RoundTable
import infrastructure.persistence.exposed.table.SessionTable
import infrastructure.persistence.exposed.table.SpinTable
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import shared.Logger
import shared.value.Currency

class WalletEngineAdapter(
    private val host: String,
    private val port: Int
) : WalletAdapter {

    private val channel: ManagedChannel by lazy {
        ManagedChannelBuilder
            .forAddress(host, port)
            .usePlaintext()
            .build()
    }

    private val stub: WalletServiceGrpc.WalletServiceBlockingStub by lazy {
        WalletServiceGrpc.newBlockingStub(channel)
    }

    override suspend fun findBalance(playerId: String): Result<Balance> = runCatching {
        BalanceCache.get(playerId)?.let { cached ->
            Logger.info("[CACHE HIT] balance for player=$playerId")
            return@runCatching cached
        }

        val request = GetAccountRequest.newBuilder()
            .setPlayerId(playerId)
            .build()

        val account = Logger.profileSuspend("walletEngine.findBalance") {
            withContext(Dispatchers.IO) { stub.getAccount(request) }
        }

        val balance = account.toBalance()

        BalanceCache.put(playerId, balance)

        balance
    }

    override suspend fun withdraw(
        playerId: String,
        transactionId: String,
        currency: Currency,
        realAmount: Long,
        bonusAmount: Long
    ): Result<Balance> = runCatching {
        val request = WithdrawRequest.newBuilder()
            .setPlayerId(playerId)
            .setCurrencyCode(currency.value)
            .setRealAmount(realAmount)
            .setBonusAmount(bonusAmount)
            .setExternalId(transactionId)
            .setType(TransactionType.TRANSACTION_TYPE_SPIN)
            .build()

        val response = Logger.profileSuspend("walletEngine.withdraw") {
            withContext(Dispatchers.IO) { stub.withdraw(request) }
        }

        val balance = response.account.toBalance()

        BalanceCache.put(playerId, balance)

        balance
    }

    override suspend fun deposit(
        playerId: String,
        transactionId: String,
        currency: Currency,
        realAmount: Long,
        bonusAmount: Long
    ): Result<Balance> = runCatching {
        val request = DepositRequest.newBuilder()
            .setPlayerId(playerId)
            .setCurrencyCode(currency.value)
            .setRealAmount(realAmount)
            .setBonusAmount(bonusAmount)
            .setExternalId(transactionId)
            .setType(TransactionType.TRANSACTION_TYPE_SPIN)
            .build()

        val response = Logger.profileSuspend("walletEngine.deposit") {
            withContext(Dispatchers.IO) { stub.deposit(request) }
        }

        val balance = response.account.toBalance()

        BalanceCache.put(playerId, balance)

        balance
    }

    override suspend fun rollback(playerId: String, transactionId: String): Result<Unit> = runCatching {
        val spinData = findSpinWithCurrency(transactionId)
            ?: throw IllegalStateException("Spin not found for transactionId=$transactionId")

        Logger.profileSuspend("walletEngine.rollback") {
            when (spinData.type) {
                SpinType.PLACE -> {
                    // PLACE was a withdraw, so rollback = deposit (refund)
                    val request = DepositRequest.newBuilder()
                        .setPlayerId(playerId)
                        .setCurrencyCode(spinData.currencyCode)
                        .setRealAmount(spinData.realAmount)
                        .setBonusAmount(spinData.bonusAmount)
                        .setExternalId(transactionId)
                        .setType(TransactionType.TRANSACTION_TYPE_CORRECT)
                        .build()
                    withContext(Dispatchers.IO) { stub.deposit(request) }
                }
                SpinType.SETTLE -> {
                    // SETTLE was a deposit, so rollback = withdraw (take back)
                    val request = WithdrawRequest.newBuilder()
                        .setPlayerId(playerId)
                        .setCurrencyCode(spinData.currencyCode)
                        .setRealAmount(spinData.realAmount)
                        .setBonusAmount(spinData.bonusAmount)
                        .setExternalId(transactionId)
                        .setType(TransactionType.TRANSACTION_TYPE_CORRECT)
                        .build()
                    withContext(Dispatchers.IO) { stub.withdraw(request) }
                }
                SpinType.ROLLBACK -> {
                    Logger.info("Ignoring rollback of already rolled-back spin transactionId=$transactionId")
                }
            }
        }

        BalanceCache.invalidate(playerId)
    }

    private data class SpinData(
        val type: SpinType,
        val realAmount: Long,
        val bonusAmount: Long,
        val currencyCode: String
    )

    private suspend fun findSpinWithCurrency(extId: String): SpinData? =
        newSuspendedTransaction {
            (SpinTable innerJoin RoundTable innerJoin SessionTable)
                .selectAll()
                .where {
                    (SpinTable.extId eq extId) and (SpinTable.roundId eq RoundTable.id) and (RoundTable.sessionId eq SessionTable.id)
                }
                .singleOrNull()
                ?.let { row ->
                    SpinData(
                        type = row[SpinTable.type],
                        realAmount = row[SpinTable.realAmount] ?: 0L,
                        bonusAmount = row[SpinTable.bonusAmount] ?: 0L,
                        currencyCode = row[SessionTable.currency]
                    )
                }
        }

    private fun Account.toBalance(): Balance = Balance(
        real = realBalance,
        bonus = bonusBalance,
        currency = Currency(currencyCode)
    )
}
