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

class WaitingTimeTracker(
    @AppCoroutineScope private val appCoroutineScope: CoroutineScope,
    private val dispatchers: CoroutineDispatchers,
    private val currentTimeInMillis: () -> Long,
    private val waitingTimeout: Long
) {
    /***
     * Injected constructor as secondary to allow default values for the parameters
     * without causing building issues with Hilt
     */
    @Inject constructor(
        @AppCoroutineScope appCoroutineScope: CoroutineScope,
        dispatchers: CoroutineDispatchers
    ) : this(appCoroutineScope, dispatchers, System::currentTimeMillis, 10000L)

    private val stateFlow: MutableStateFlow<State> = MutableStateFlow(Idle)
    val currentState: State get() = stateFlow.value

    private var waitingJob: Job? = null

    fun onWaitingStarted(onWaitingTimeAvailable: (Long) -> Unit = {}) {
        if (currentState is Waiting) return

        waitingJob?.cancel()
        stateFlow.update { Waiting(currentTimeInMillis()) }

        waitingJob = appCoroutineScope.launch(dispatchers.computation) {
            waitForDoneState(onWaitingTimeAvailable, currentState.creationTimestamp)
                .exceptionOrNull()
                ?.let {
                    stateFlow.update { Idle }
                    waitingJob = null
                }
        }

    }

    private suspend fun waitForDoneState(
        onWaitingTimeAvailable: (Long) -> Unit,
        waitingStartedTimestamp: Long
    ) = runCatching {
            withTimeout(waitingTimeout) {
                stateFlow.collectLatest {
                    if (it is Done) {
                        onWaitingTimeAvailable(
                            it.creationTimestamp - waitingStartedTimestamp
                        )
                        cancel()
                    }
                }
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
