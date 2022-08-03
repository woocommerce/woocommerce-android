package com.woocommerce.android.analytics

import com.woocommerce.android.analytics.WaitingTimeTracker.State.Done
import com.woocommerce.android.di.AppCoroutineScope
import com.woocommerce.android.util.CoroutineDispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

class WaitingTimeTracker @Inject constructor(
    @AppCoroutineScope private val appCoroutineScope: CoroutineScope,
    private val dispatchers: CoroutineDispatchers,
    private val currentTimeInMillis: () -> Long = System::currentTimeMillis,
    private val waitingTimeout: Long = 10000L
) {
    var state: MutableStateFlow<State> = MutableStateFlow(State.Idle)

    suspend fun onWaitingStarted() {
        state.update { State.Waiting(currentTimeInMillis()) }
        appCoroutineScope.launch(dispatchers.computation) {

            withTimeout(waitingTimeout) {
                state.collectLatest {
                    if (it is Done) {
                        state.update { State.Idle }
                        cancel()
                        // publish waiting time
                    }
                }
            }

            state.update { State.Idle }
        }

    }

    suspend fun onWaitingEnded() {
        state.emit(Done(currentTimeInMillis()))
    }

    sealed class State(val creationTimestamp: Long) {
        object Idle : State(0L)
        class Waiting(creationTimestamp: Long) : State(creationTimestamp)
        class Done(creationTimestamp: Long) : State(creationTimestamp)
    }
}
