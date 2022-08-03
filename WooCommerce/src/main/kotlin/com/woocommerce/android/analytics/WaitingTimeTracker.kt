package com.woocommerce.android.analytics

import com.woocommerce.android.analytics.WaitingTimeTracker.State.Done
import com.woocommerce.android.analytics.WaitingTimeTracker.State.Idle
import com.woocommerce.android.analytics.WaitingTimeTracker.State.Waiting
import com.woocommerce.android.di.AppCoroutineScope
import com.woocommerce.android.util.CoroutineDispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

class WaitingTimeTracker @Inject constructor(
    @AppCoroutineScope private val appCoroutineScope: CoroutineScope,
    private val dispatchers: CoroutineDispatchers,
    private val currentTimeInMillis: () -> Long = System::currentTimeMillis,
    private val waitingTimeout: Long = 10000L
) {
    private val stateFlow: MutableStateFlow<State> = MutableStateFlow(Idle)
    val currentState: State get() = stateFlow.value

    private var waitingJob: Job? = null

    suspend fun onWaitingStarted() {
        if (currentState is Waiting) return

        waitingJob?.cancel()
        stateFlow.update { Waiting(currentTimeInMillis()) }
        waitingJob = appCoroutineScope.launch(dispatchers.computation) {

            withTimeout(waitingTimeout) {
                stateFlow.collectLatest {
                    if (it is Done) {
                        stateFlow.update { Idle }
                        waitingJob = null
                        cancel()
                        // publish waiting time
                    }
                }
            }

            stateFlow.update { Idle }
            waitingJob = null
        }

    }

    suspend fun onWaitingEnded() {
        if (currentState is Waiting) {
            stateFlow.emit(Done(currentTimeInMillis()))
        }
    }

    sealed class State(val creationTimestamp: Long) {
        object Idle : State(0L)
        class Waiting(creationTimestamp: Long) : State(creationTimestamp)
        class Done(creationTimestamp: Long) : State(creationTimestamp)
    }
}
