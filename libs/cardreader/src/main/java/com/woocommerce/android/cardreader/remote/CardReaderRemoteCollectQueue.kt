package com.woocommerce.android.cardreader.remote

import com.woocommerce.android.cardreader.LogWrapper
import com.woocommerce.android.cardreader.describeWithCauses
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

internal class CardReaderRemoteCollectQueue(
    private val scope: CoroutineScope,
    private val logWrapper: LogWrapper,
) {
    private val oneCollectAtATime = Mutex()
    private val jobs = ConcurrentHashMap<String, CollectJob>()

    @Volatile private var lastStartedRequestId: String? = null

    private val stripeCancellationFailureHandler = CoroutineExceptionHandler { _, throwable ->
        logWrapper.e(TAG, "Stripe failed to cancel the collect: ${throwable.describeWithCauses()}")
    }

    fun enqueue(requestId: String, collect: suspend () -> Unit) {
        val pending = CollectJob()
        val job = scope.launch(stripeCancellationFailureHandler, start = CoroutineStart.LAZY) {
            oneCollectAtATime.withLock {
                if (!pending.stage.compareAndSet(Stage.QUEUED, Stage.STARTED)) return@withLock
                lastStartedRequestId = requestId
                collect()
            }
        }
        pending.job = job
        jobs[requestId] = pending
        job.invokeOnCompletion { jobs.remove(requestId, pending) }
        job.start()
    }

    fun isCurrent(requestId: String): Boolean = lastStartedRequestId == requestId

    fun cancelQueued(requestId: String): Boolean {
        val pending = jobs[requestId] ?: return false
        if (!pending.stage.compareAndSet(Stage.QUEUED, Stage.CANCELLED)) return false
        pending.job?.cancel()
        return true
    }

    fun cancel(requestId: String) {
        jobs[requestId]?.job?.cancel()
    }

    fun cancelAll() {
        jobs.values.forEach { it.job?.cancel() }
    }

    private class CollectJob {
        val stage = AtomicReference(Stage.QUEUED)
        var job: Job? = null
    }

    private enum class Stage { QUEUED, STARTED, CANCELLED }

    private companion object {
        const val TAG = "CardReaderRemoteCollectQueue"
    }
}
