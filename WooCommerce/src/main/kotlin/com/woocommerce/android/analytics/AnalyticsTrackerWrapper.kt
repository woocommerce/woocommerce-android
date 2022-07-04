package com.woocommerce.android.analytics

import dagger.Reusable
import javax.inject.Inject

@Reusable
class AnalyticsTrackerWrapper
@Inject constructor() {
    fun track(stat: AnalyticsEvent, properties: Map<String, *> = emptyMap<String, Any>()) {
        AnalyticsTracker.track(stat, properties)
    }

    /**
     * A convenience method for logging an error event with some additional meta data.
     * @param stat The stat to track.
     * @param errorContext A string providing additional context (if any) about the error.
     * @param errorType The type of error.
     * @param errorDescription The error text or other description.
     */
    fun track(stat: AnalyticsEvent, errorContext: String?, errorType: String?, errorDescription: String?) {
        AnalyticsTracker.track(stat, errorContext, errorType, errorDescription)
    }

    /**
     * A convenience method for logging an error event with some additional meta data.
     * @param stat The stat to track.
     * @param properties Map of additional properties
     * @param errorContext A string providing additional context (if any) about the error.
     * @param errorType The type of error.
     * @param errorDescription The error text or other description.
     */
    fun track(
        stat: AnalyticsEvent,
        properties: Map<String, Any>,
        errorContext: String?,
        errorType: String?,
        errorDescription: String?
    ) {
        AnalyticsTracker.track(stat, properties, errorContext, errorType, errorDescription)
    }

    /**
     * A convenience method for tracking views shown during a session.
     * @param view The view to be tracked
     */
    fun trackViewShown(view: Any) = AnalyticsTracker.trackViewShown(view)

    fun flush() {
        AnalyticsTracker.flush()
    }
}
