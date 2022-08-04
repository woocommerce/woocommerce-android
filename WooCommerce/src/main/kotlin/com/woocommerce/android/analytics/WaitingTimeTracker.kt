package com.woocommerce.android.analytics

class WaitingTimeTracker(
    private val trackEvent: AnalyticsEvent,
    private val currentTimeInMillis: () -> Long = System::currentTimeMillis
) {
    private var waitingStartTimestamp: Long? = null

    private val Long.elapsedWaitingTime
        get() = (currentTimeInMillis() - this).toDouble() / IN_SECONDS

    fun start() {
        waitingStartTimestamp = currentTimeInMillis()
    }

    fun end() = waitingStartTimestamp?.elapsedWaitingTime?.let {
        AnalyticsTracker.track(
            trackEvent,
            mapOf(AnalyticsTracker.KEY_WAITING_TIME to it)
        )
        waitingStartTimestamp = null
    }

    fun abort() {
        waitingStartTimestamp = null
    }

    companion object {
        const val IN_SECONDS = 1000
    }
}
