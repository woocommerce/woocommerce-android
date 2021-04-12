package com.woocommerce.android.util

import com.woocommerce.android.AppConstants
import com.woocommerce.android.util.ContinuationWrapper.ContinuationResult.Cancellation
import com.woocommerce.android.util.ContinuationWrapper.ContinuationResult.Success
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.resume

/**
 * A wrapper class for a [CancellableContinuation], which handles some of the most common errors when a continuation
 * is used.
 *
 * 1. First, before a continuation is resumed, it's checked if it's active.
 * 2. After a continuation is successfully resumed it's nulled.
 * 3. The whole async call is wrapped in a try-catch block which handles the [CancellationException].
 *
 * There is a required tag parameter because we want to make sure all [CancellationException] are logged.
 *
 */
class ContinuationWrapper<T>(private val tag: WooLog.T) {
    private var continuation: CancellableContinuation<T>? = null

    val isWaiting: Boolean
        @Synchronized
        get() = continuation?.isActive ?: false

    suspend fun callAndWaitUntilTimeout(
        timeout: Long = AppConstants.REQUEST_TIMEOUT,
        asyncAction: () -> Unit
    ): ContinuationResult<T> {
        return callAndWait(asyncAction, timeout)
    }

    suspend fun callAndWait(asyncAction: () -> Unit): ContinuationResult<T> {
        return callAndWait(asyncAction, 0)
    }

    @Synchronized
    private suspend fun callAndWait(asyncAction: () -> Unit, timeout: Long): ContinuationResult<T> {
        suspend fun suspendCoroutine(asyncRequest: () -> Unit) = suspendCancellableCoroutine<T> {
            continuation = it
            asyncRequest()
        }

        return try {
            continuation?.cancel()
            val continuationResult = if (timeout > 0) {
                withTimeout(timeout) {
                    suspendCoroutine(asyncAction)
                }
            } else {
                suspendCoroutine(asyncAction)
            }
            Success(continuationResult)
        } catch (e: CancellationException) {
            WooLog.e(tag, e)
            Cancellation<T>(e)
        } finally {
            continuation = null
        }
    }

    @Synchronized
    fun continueWith(value: T) {
        if (continuation?.isActive == true) {
            continuation?.resume(value)
        }
    }

    @Synchronized
    fun cancel() {
        continuation?.cancel()
    }

    sealed class ContinuationResult<T> {
        data class Success<T>(val value: T) : ContinuationResult<T>()
        data class Cancellation<T>(val exception: CancellationException) : ContinuationResult<T>()
    }
}
