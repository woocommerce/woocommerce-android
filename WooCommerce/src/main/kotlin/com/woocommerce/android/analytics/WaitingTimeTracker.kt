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
    @Inject
    constructor(
        @AppCoroutineScope appCoroutineScope: CoroutineScope,
        dispatchers: CoroutineDispatchers
    ) : this(appCoroutineScope, dispatchers, System::currentTimeMillis, DEFAULT_WAITING_TIMEOUT)

    private val stateFlow: MutableStateFlow<State> = MutableStateFlow(Idle)
    val currentState: State get() = stateFlow.value

    private var waitingJob: Job? = null

    /***
     * Trigger a new waiting job if it is not already running,
     * and returns the current state to `Idle` after the waiting expires
     */
    suspend fun onWaitingStarted(trackEvent: AnalyticsEvent) {
        if (currentState is Waiting) return

        waitingJob?.cancel()
        stateFlow.update { Waiting(currentTimeInMillis()) }

        waitingJob = appCoroutineScope.launch(dispatchers.computation) {
            waitForDoneState(trackEvent, currentState.creationTimestamp)
                .exceptionOrNull()
                ?.let {
                    stateFlow.update { Idle }
                    waitingJob = null
                }
        }
    }

    /***
     * Emits to the current state `Done` if it is already waiting
     * to collect it
     */
    suspend fun onWaitingEnded() {
        if (currentState is Waiting) {
            stateFlow.emit(Done(currentTimeInMillis()))
        }
    }

    /***
     * Start observing the state flow until the timeout expires,
     * if the `Done` state is emitted, then handle it and immediately cancel
     * the waiting job.
     *
     * Since only possible results for this job is to expire or be canceled,
     * the operation is wrapped it under a failure result, so the caller can
     * be notified that the waiting cycle ended.
     */
    private suspend fun waitForDoneState(
        trackEvent: AnalyticsEvent,
        waitingStartedTimestamp: Long
    ) = runCatching {
        withTimeout(waitingTimeout) {
            stateFlow.collectLatest {
                if (it is Done) {
                    val waitingTimeElapsed = it.creationTimestamp - waitingStartedTimestamp
                    if (waitingTimeElapsed in 1..waitingTimeout) {
                        sendWaitingTimeToTracks(trackEvent, waitingTimeElapsed)
                    }
                    cancel()
                }
            }
        }
    }

    private fun sendWaitingTimeToTracks(
        trackEvent: AnalyticsEvent,
        waitingTimeElapsed: Long
    ) {
        AnalyticsTracker.track(
            trackEvent,
            mapOf(AnalyticsTracker.KEY_WAITING_TIME to waitingTimeElapsed)
        )
    }

    sealed class State(val creationTimestamp: Long) {
        object Idle : State(0L)
        class Waiting(creationTimestamp: Long) : State(creationTimestamp)
        class Done(creationTimestamp: Long) : State(creationTimestamp)
    }

    companion object {
        const val DEFAULT_WAITING_TIMEOUT = 30000L
    }
}
