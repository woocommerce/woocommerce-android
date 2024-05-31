package com.woocommerce.android.analytics

import android.content.SharedPreferences
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.PREFKEY_SEND_USAGE_STATS
import dagger.Reusable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

@Reusable
open class AnalyticsTrackerWrapper @Inject constructor() {
    open var sendUsageStats: Boolean by AnalyticsTracker.Companion::sendUsageStats

    open fun observeSendUsageStats(): Flow<Boolean> {
        return callbackFlow {
            val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                if (key == PREFKEY_SEND_USAGE_STATS) {
                    trySend(sendUsageStats)
                }
            }
            AppPrefs.getPreferences().registerOnSharedPreferenceChangeListener(listener)

            awaitClose {
                AppPrefs.getPreferences().unregisterOnSharedPreferenceChangeListener(listener)
            }
        }.onStart {
            emit(sendUsageStats)
        }
    }

    fun track(stat: IAnalyticsEvent, properties: Map<String, *> = emptyMap<String, Any>()) {
        AnalyticsTracker.track(stat, properties)
    }

    /**
     * A convenience method for logging an error event with some additional meta data.
     * @param stat The stat to track.
     * @param errorContext A string providing additional context (if any) about the error.
     * @param errorType The type of error.
     * @param errorDescription The error text or other description.
     */
    fun track(stat: IAnalyticsEvent, errorContext: String?, errorType: String?, errorDescription: String?) {
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
        stat: IAnalyticsEvent,
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
