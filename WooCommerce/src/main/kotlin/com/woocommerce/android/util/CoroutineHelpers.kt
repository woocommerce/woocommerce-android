package com.woocommerce.android.util

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.Continuation

/**
 * Runs a given suspending block of code inside a coroutine with a specified [timeout][timeMillis] and returns
 * `null` if this timeout was exceeded.
 *
 * @throws [CancellationException] if the coroutine is cancelled or completed while suspended.
 */
suspend inline fun <T> suspendCoroutineWithTimeout(
    timeout: Long,
    crossinline block: (Continuation<T>) -> Unit
) = coroutineScope {
    withTimeoutOrNull(timeout) {
        suspendCancellableCoroutine(block = block)
    }
}

/**
 * Similar to the above but returns a cancellable continuation
 */
suspend inline fun <T> suspendCancellableCoroutineWithTimeout(
    timeout: Long,
    crossinline block: (CancellableContinuation<T>) -> Unit
) = coroutineScope {
    withTimeoutOrNull(timeout) {
        suspendCancellableCoroutine(block = block)
    }
}

/**
 * Waits for the completion of the first [Deferred] from the passed list, then return its result.
 */
suspend inline fun <T> Iterable<Deferred<T>>.awaitAny(autoCancelRemainingTasks: Boolean = true): T {
    val firstResult = select<T> {
        forEach { deferred ->
            deferred.onAwait { it }
        }
    }

    if (autoCancelRemainingTasks) {
        filter { !it.isCancelled && !it.isCancelled }.forEach { it.cancel() }
    }
    return firstResult
}
