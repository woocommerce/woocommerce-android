package com.woocommerce.android.cardreader.remote

import com.woocommerce.android.cardreader.LogWrapper
import com.woocommerce.android.cardreader.describeWithCauses
import kotlinx.coroutines.CompletionHandlerException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

internal class CardReaderRemoteCollectQueue(
    private val scope: CoroutineScope,
    private val logWrapper: LogWrapper,
) {
    private val oneCollectAtATime = Mutex()
    private val jobs = ConcurrentHashMap<String, Job>()

    @OptIn(InternalCoroutinesApi::class)
    private val stripeCancellationFailureHandler = CoroutineExceptionHandler { _, throwable ->
        if (throwable !is CompletionHandlerException) throw throwable
        logWrapper.e(TAG, "Stripe failed to cancel the collect: ${throwable.describeWithCauses()}")
    }

    fun enqueue(requestId: String, collect: suspend () -> Unit) {
        val job = scope.launch(stripeCancellationFailureHandler) {
            oneCollectAtATime.withLock { collect() }
        }
        jobs[requestId] = job
        job.invokeOnCompletion { jobs.remove(requestId) }
    }

    fun cancel(requestId: String) {
        jobs[requestId]?.cancel()
    }

    fun cancelAll() {
        jobs.values.forEach { it.cancel() }
    }

    private companion object {
        const val TAG = "CardReaderRemoteCollectQueue"
    }
}
