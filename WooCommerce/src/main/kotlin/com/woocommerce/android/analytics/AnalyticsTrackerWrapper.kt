package com.woocommerce.android.analytics

import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import dagger.Reusable
import javax.inject.Inject

@Reusable
class AnalyticsTrackerWrapper
@Inject constructor() {
    fun track(stat: Stat) {
        AnalyticsTracker.track(stat)
    }

    fun track(stat: Stat, properties: Map<String, *>) {
        AnalyticsTracker.track(stat, properties)
    }

    /**
     * A convenience method for logging an error event with some additional meta data.
     * @param stat The stat to track.
     * @param errorContext A string providing additional context (if any) about the error.
     * @param errorType The type of error.
     * @param errorDescription The error text or other description.
     */
    fun track(stat: Stat, errorContext: String, errorType: String, errorDescription: String) {
        AnalyticsTracker.track(stat, errorContext, errorType, errorDescription)
    }
}
