package infrastructure.persistence

import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

/**
 * Runs [block] inside an Exposed suspended transaction.
 *
 * Single seam for every write path in the codebase. Prefer this over importing
 * `newSuspendedTransaction` directly so that transaction policy (isolation, read-only,
 * logging) stays in one place.
 */
suspend fun <T> dbTransaction(block: suspend Transaction.() -> T): T =
    newSuspendedTransaction(statement = block)

/**
 * Runs [block] inside a read-only Exposed suspended transaction. Use from every
 * query handler / repository find* method. The read-only hint lets the database
 * optimizer skip write-path bookkeeping.
 */
suspend fun <T> dbRead(block: suspend Transaction.() -> T): T =
    newSuspendedTransaction(readOnly = true, statement = block)
