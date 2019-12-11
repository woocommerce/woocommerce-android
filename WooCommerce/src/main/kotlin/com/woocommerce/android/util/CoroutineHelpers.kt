package com.woocommerce.android.util

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
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
