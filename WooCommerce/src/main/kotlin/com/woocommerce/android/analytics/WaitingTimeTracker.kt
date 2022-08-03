package com.woocommerce.android.analytics

class WaitingTimeTracker {
    private var state: State = State.Idle

    fun onWaitingStarted() {

    }

    fun onWaitingEnded() {

    }

    sealed class State {
        object Idle : State()
        object Waiting : State()
        object Done : State()
    }
}
