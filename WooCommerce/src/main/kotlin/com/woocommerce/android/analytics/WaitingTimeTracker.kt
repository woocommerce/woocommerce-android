package com.woocommerce.android.analytics

class WaitingTimeTracker(
    private val currentTimeInMillis: () -> Long
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
