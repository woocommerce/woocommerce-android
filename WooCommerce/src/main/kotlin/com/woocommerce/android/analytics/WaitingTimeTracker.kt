package com.woocommerce.android.analytics

import com.woocommerce.android.util.CoroutineDispatchers
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

class WaitingTimeTracker @Inject constructor(
    private val dispatchers: CoroutineDispatchers,
    private val currentTimeInMillis: () -> Long,
    private val waitingTimeout: Long = 10000L
) {
    private var state: State = State.Idle

    fun onWaitingStarted() {
    }

    fun onWaitingEnded() {

    }

    private sealed class State(val creationTimestamp: Long) {
        object Idle : State(0L)
        class Waiting(creationTimestamp: Long) : State(creationTimestamp)
        class Done(creationTimestamp: Long) : State(creationTimestamp)
    }
}
