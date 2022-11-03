package com.woocommerce.android.ui.jitm

import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.JITM_FEATURE_CLASS
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.JITM_ID
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_JITM
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_JITM_COUNT
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_SOURCE
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import javax.inject.Inject

class JitmTracker @Inject constructor(
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) {
    private fun track(
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
            properties = mapOf(
                KEY_SOURCE to source,
                KEY_JITM to (jitmId ?: "null"),
                KEY_JITM_COUNT to (jitmCount ?: 0)
            )
        )
    }

    fun trackJitmDisplayed(source: String, jitmId: String, featureClass: String) {
        track(
            stat = AnalyticsEvent.JITM_DISPLAYED,
            properties = mapOf(
                KEY_SOURCE to source,
                JITM_ID to jitmId,
                JITM_FEATURE_CLASS to featureClass
            )
        )
    }

    fun trackJitmCtaTapped(source: String, jitmId: String, featureClass: String) {
        track(
            stat = AnalyticsEvent.JITM_CTA_TAPPED,
            properties = mapOf(
                KEY_SOURCE to source,
                JITM_ID to jitmId,
                JITM_FEATURE_CLASS to featureClass
            )
        )
    }

    fun trackJitmDismissTapped(source: String, jitmId: String, featureClass: String) {
        track(
            stat = AnalyticsEvent.JITM_DISMISS_TAPPED,
            properties = mapOf(
                KEY_SOURCE to source,
                JITM_ID to jitmId,
                JITM_FEATURE_CLASS to featureClass
            )
        )
    }

    fun trackJitmDismissSuccess(source: String, jitmId: String, featureClass: String) {
        track(
            stat = AnalyticsEvent.JITM_DISMISS_SUCCESS,
            properties = mapOf(
                KEY_SOURCE to source,
                JITM_ID to jitmId,
                JITM_FEATURE_CLASS to featureClass
            )
        )
    }

    fun trackJitmDismissFailure(
        source: String,
        jitmId: String,
        featureClass: String,
        errorType: WooErrorType?,
        errorDescription: String?
    ) {
        track(
            stat = AnalyticsEvent.JITM_DISMISS_FAILURE,
            properties = mapOf(
                KEY_SOURCE to source,
                JITM_ID to jitmId,
                JITM_FEATURE_CLASS to featureClass,
            ),
            errorType = errorType?.name ?: WooErrorType.GENERIC_ERROR.name,
            errorDescription = errorDescription
        )
    }
}
