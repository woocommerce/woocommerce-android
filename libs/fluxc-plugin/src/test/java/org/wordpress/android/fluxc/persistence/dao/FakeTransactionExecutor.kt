package org.wordpress.android.fluxc.persistence.dao

import org.wordpress.android.fluxc.persistence.TransactionExecutor

object FakeTransactionExecutor : TransactionExecutor {
    override suspend fun <R> executeInTransaction(block: suspend () -> R): R = block()

    override fun <R> runInTransaction(block: () -> R): R = block()
}
