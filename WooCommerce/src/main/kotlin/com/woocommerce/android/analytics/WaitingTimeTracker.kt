package com.woocommerce.android.analytics

class WaitingTimeTracker(
    private val trackEvent: AnalyticsEvent,
    private val currentTimeInMillis: () -> Long = System::currentTimeMillis
) {
    private var waitingStartTimestamp: Long? = null

    private val Long.elapsedWaitingTime
        get() = (currentTimeInMillis() - this).toDouble() / IN_SECONDS

    fun onWaitingStarted() {
        waitingStartTimestamp = currentTimeInMillis()
    }

    fun onWaitingEnded() = waitingStartTimestamp?.elapsedWaitingTime?.let {
        AnalyticsTracker.track(
            trackEvent,
            mapOf(AnalyticsTracker.KEY_WAITING_TIME to it)
        )
        waitingStartTimestamp = null
    }

    fun onWaitingAborted() {
        waitingStartTimestamp = null
    }

    companion object {
        const val IN_SECONDS = 1000
    }
}
