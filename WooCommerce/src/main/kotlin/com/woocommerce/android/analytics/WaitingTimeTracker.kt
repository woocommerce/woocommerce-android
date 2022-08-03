package com.woocommerce.android.analytics

class WaitingTimeTracker {

    sealed class State {
        object Idle : State()
        object Waiting : State()
        object Done : State()
    }
}
