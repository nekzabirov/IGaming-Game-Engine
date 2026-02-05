package application.port.outbound.external

import domain.session.model.Balance
import shared.value.Currency

/**
 * Port interface for wallet operations.
 * Implementations connect to external wallet service.
 */
interface WalletAdapter {
    /**
     * Get player's current balance.
     */
    suspend fun findBalance(playerId: String): Result<Balance>

    /**
     * Withdraw funds from player's wallet.
     * Returns the updated balance after withdrawal.
     */
    suspend fun withdraw(
        playerId: String,
        transactionId: String,
        currency: Currency,
        realAmount: Long,
        bonusAmount: Long
    ): Result<Balance>

    /**
     * Deposit funds to player's wallet.
     * Returns the updated balance after deposit.
     */
    suspend fun deposit(
        playerId: String,
        transactionId: String,
        currency: Currency,
        realAmount: Long,
        bonusAmount: Long
    ): Result<Balance>

    /**
     * Rollback a previous transaction.
     */
    suspend fun rollback(playerId: String, transactionId: String): Result<Unit>
}
