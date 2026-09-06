package clients

import PamConfig
import com.nekgambling.pam.v1.EnsureAccountRequest
import com.nekgambling.pam.v1.FindAccountRequest
import com.nekgambling.pam.v1.TransactRequest
import com.nekgambling.pam.v1.WalletAccount
import com.nekgambling.pam.v1.WalletServiceGrpc
import errors.WalletUnavailableException
import io.grpc.CallOptions
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.MethodDescriptor
import io.grpc.Status
import io.grpc.StatusException
import io.grpc.kotlin.ClientCalls
import java.util.concurrent.TimeUnit

data class Balance(
    val real: Long,
    val bonus: Long,
    val currency: String,
) {
    val total: Long get() = real + bonus
}

/** The player's purse. Amounts are the wallet system unit (nano). */
interface Wallet {

    suspend fun balance(playerId: String, currency: String): Balance

    /**
     * ONE signed movement, idempotent by [reference]: a retry of the same reference moves nothing
     * and answers the balance the first call produced. Negative amounts take money, positive give.
     * Returns the balance AFTER the movement — the wallet's own answer, not a projection.
     */
    suspend fun transact(playerId: String, reference: String, currency: String, realAmount: Long, bonusAmount: Long): Balance
}

/**
 * The wallet ledger inside pam-engine. Money moves on (player, currency): pam resolves the purse
 * from the pair and mints it on the first movement, so nothing is looked up or cached here.
 * Calls go through grpc-kotlin's suspending `unaryRpc`, so no thread blocks on the network.
 */
class PamWallet(config: PamConfig) : Wallet {

    private val channel: ManagedChannel = ManagedChannelBuilder
        .forAddress(config.host, config.port)
        .usePlaintext()
        .keepAliveTime(30, TimeUnit.SECONDS)
        .keepAliveTimeout(10, TimeUnit.SECONDS)
        .keepAliveWithoutCalls(true)
        .build()
        // Resolve and connect now, at boot, so the first money call of a fresh pod does not pay
        // for it while the hub is waiting.
        .also { it.getState(true) }

    override suspend fun balance(playerId: String, currency: String): Balance = guarded {
        account(playerId, currency).toBalance(currency)
    }

    override suspend fun transact(
        playerId: String,
        reference: String,
        currency: String,
        realAmount: Long,
        bonusAmount: Long,
    ): Balance {
        // Transact refuses a movement of nothing, and a zero move is exactly what a losing settle
        // is: report the balance the caller would have read anyway instead of failing the spin.
        if (realAmount == 0L && bonusAmount == 0L) return balance(playerId, currency)

        val request = TransactRequest.newBuilder()
            .setUserId(playerId.toLong())
            .setCurrency(currency)
            .setReference(reference)
            .setType(TYPE)
            .setRealAmount(realAmount)
            .setBonusAmount(bonusAmount)
            .build()

        return guarded { call(WalletServiceGrpc.getTransactMethod(), request).account.toBalance(currency) }
    }

    fun close() {
        channel.shutdown()
    }

    /** A balance read still needs the purse to exist — a miss mints it (idempotent) and reads again. */
    private suspend fun account(playerId: String, currency: String): WalletAccount {
        val request = FindAccountRequest.newBuilder()
            .setUserId(playerId.toLong())
            .setCurrency(currency)
            .build()

        return try {
            call(WalletServiceGrpc.getFindAccountMethod(), request)
        } catch (e: StatusException) {
            if (e.status.code != Status.Code.NOT_FOUND) throw e
            call(
                WalletServiceGrpc.getEnsureAccountMethod(),
                EnsureAccountRequest.newBuilder().setUserId(playerId.toLong()).setCurrency(currency).build(),
            )
            call(WalletServiceGrpc.getFindAccountMethod(), request)
        }
    }

    /** The wallet answered with a failure or not at all: an outage, reported as such. */
    private suspend fun <T> guarded(block: suspend () -> T): T =
        try {
            block()
        } catch (e: StatusException) {
            throw WalletUnavailableException(e.message)
        }

    private suspend fun <Req, Res> call(method: MethodDescriptor<Req, Res>, request: Req): Res =
        ClientCalls.unaryRpc(
            channel,
            method,
            request,
            CallOptions.DEFAULT.withDeadlineAfter(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS),
        )

    private fun WalletAccount.toBalance(currency: String): Balance =
        Balance(real = realBalance, bonus = bonusBalance, currency = currency)

    private companion object {
        /** The ledger category every casino and sportbook movement has always been recorded under. */
        const val TYPE = "SPIN"

        /** Shorter than the hub's own patience for an operator call, so a stuck wallet answers
         *  INTERNAL (and is retried under the same leg id) instead of timing out on the hub's side. */
        const val CALL_TIMEOUT_SECONDS = 6L
    }
}
