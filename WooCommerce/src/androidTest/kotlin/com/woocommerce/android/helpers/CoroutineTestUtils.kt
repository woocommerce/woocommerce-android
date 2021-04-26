package com.woocommerce.android.helpers

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.resume

@Deprecated(
    message = "Use runBlockingTest which provides additional features such as skipping delays",
    replaceWith = ReplaceWith("runBlockingTest", "import kotlinx.coroutines.test.runBlockingTest")
)
fun <T> test(context: CoroutineContext = EmptyCoroutineContext, block: suspend CoroutineScope.() -> T) {
    runBlocking(context, block)
}

@ExperimentalCoroutinesApi val TEST_SCOPE = CoroutineScope(Dispatchers.Unconfined)
@InternalCoroutinesApi val TEST_DISPATCHER: CoroutineDispatcher = TestDispatcher()

@InternalCoroutinesApi
private class TestDispatcher : CoroutineDispatcher(), Delay {
    @InternalCoroutinesApi
    override fun scheduleResumeAfterDelay(timeMillis: Long, continuation: CancellableContinuation<Unit>) {
        continuation.resume(Unit)
    }

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        block.run()
    }
}
