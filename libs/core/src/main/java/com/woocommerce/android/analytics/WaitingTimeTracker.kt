package com.woocommerce.android.analytics

class WaitingTimeTracker(
    private val trackEvent: AnalyticsEvent,
    private val currentTimeInMillis: () -> Long = System::currentTimeMillis
) {
    private var waitingStartedTimestamp: Long? = null

    private val Long.elapsedWaitingTime
        get() = (currentTimeInMillis() - this).toDouble() / IN_SECONDS

    fun start() {
        waitingStartedTimestamp = currentTimeInMillis()
    }

    fun end(additionalProperties: Map<String, *> = emptyMap<String, String>()) =
        waitingStartedTimestamp?.elapsedWaitingTime?.let {
            AnalyticsTracker.track(
                trackEvent,
                additionalProperties + mapOf(AnalyticsTracker.KEY_WAITING_TIME to it)
            )
            waitingStartedTimestamp = null
        }

    fun abort() {
        waitingStartedTimestamp = null
    }

    companion object {
        const val IN_SECONDS = 1000
    }
}
