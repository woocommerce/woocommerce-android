package com.woocommerce.android.ui

import androidx.annotation.VisibleForTesting
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_JITM
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_JITM_COUNT
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_SOURCE
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import javax.inject.Inject

class JitmTracker @Inject constructor(
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) {
    @VisibleForTesting
    fun track(
        stat: AnalyticsEvent,
        properties: Map<String, Any> = mapOf(),
        errorType: String? = null,
        errorDescription: String? = null,
    ) {
        val isError = !errorType.isNullOrBlank() || !errorDescription.isNullOrEmpty()
        if (isError) {
            analyticsTrackerWrapper.track(
                stat,
                properties,
                this@JitmTracker.javaClass.simpleName,
                errorType,
                errorDescription
            )
        } else {
            analyticsTrackerWrapper.track(stat, properties)
        }
    }

    fun trackJitmFetchFailure(source: String, type: WooErrorType, message: String?) {
        track(
            stat = AnalyticsEvent.JITM_FETCH_FAILURE,
            properties = mapOf(KEY_SOURCE to source),
            errorType = type.name,
            errorDescription = message
        )
    }

    fun trackJitmFetchSuccess(source: String, jitmId: String?, jitmCount: Int?) {
        track(
            stat = AnalyticsEvent.JITM_FETCH_SUCCESS,
            properties = mutableMapOf(
                KEY_SOURCE to source,
                KEY_JITM to (jitmId ?: "null"),
                KEY_JITM_COUNT to (jitmCount ?: 0)
            )
        )
    }
}
