package com.woocommerce.android.analytics

class WaitingTimeTracker(
) {
    private var state: State = State.Idle(0L)

    fun onWaitingStarted() {

    }

    fun onWaitingEnded() {

    }

    private sealed class State(val creationTimestamp: Long) {
        class Idle(creationTimestamp: Long) : State(creationTimestamp)
        class Waiting(creationTimestamp: Long) : State(creationTimestamp)
        class Done(creationTimestamp: Long) : State(creationTimestamp)
    }
}
