package org.wordpress.android.fluxc.persistence

interface TransactionExecutor {
    suspend fun <R> executeInTransaction(block: suspend () -> R): R
    fun <R> runInTransaction(block: () -> R): R
}
