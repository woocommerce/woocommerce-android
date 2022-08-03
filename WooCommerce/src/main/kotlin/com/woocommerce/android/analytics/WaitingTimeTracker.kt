package com.woocommerce.android.analytics

import com.woocommerce.android.analytics.WaitingTimeTracker.State.Done
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withTimeout

class WaitingTimeTracker(
    private val currentTimeInMillis: () -> Long = System::currentTimeMillis,
    private val waitingTimeout: Long = 10000L
) {
    private var state: MutableStateFlow<State> = MutableStateFlow(State.Idle)

    suspend fun onWaitingStarted() {
        withTimeout(waitingTimeout) {
            state.update { State.Waiting(currentTimeInMillis()) }
            state.collectLatest {
                if (it is Done) {
                    // publish waiting time
                }
            }
        }
        state.update { State.Idle }

    }

    suspend fun onWaitingEnded() {
        state.emit(Done(currentTimeInMillis()))
    }

    private sealed class State(val creationTimestamp: Long) {
        object Idle : State(0L)
        class Waiting(creationTimestamp: Long) : State(creationTimestamp)
        class Done(creationTimestamp: Long) : State(creationTimestamp)
    }
}
