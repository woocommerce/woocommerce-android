package com.woocommerce.android.analytics

import com.woocommerce.android.util.CoroutineDispatchers
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

class WaitingTimeTracker(
    private val currentTimeInMillis: () -> Long = System::currentTimeMillis,
    private val waitingTimeout: Long = 10000L
) {
    private var state: State = State.Idle

    suspend fun onWaitingStarted() {
        withTimeoutOrNull(waitingTimeout) {
            state = State.Waiting(currentTimeInMillis())
        }
    }

    fun onWaitingEnded() {

    }

    private sealed class State(val creationTimestamp: Long) {
        object Idle : State(0L)
        class Waiting(creationTimestamp: Long) : State(creationTimestamp)
        class Done(creationTimestamp: Long) : State(creationTimestamp)
    }
}
