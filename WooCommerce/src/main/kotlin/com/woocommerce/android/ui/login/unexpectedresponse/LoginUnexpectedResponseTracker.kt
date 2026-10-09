package com.woocommerce.android.ui.login.unexpectedresponse

import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import java.util.Locale
import javax.inject.Inject

class LoginUnexpectedResponseTracker @Inject constructor(
    private val analyticsTracker: AnalyticsTrackerWrapper
) {
    fun trackErrorShown(failure: LoginUnexpectedResponseFailure) {
        track(AnalyticsEvent.LOGIN_UNEXPECTED_RESPONSE_ERROR_SHOWN, failure)
    }

    fun trackActionTapped(failure: LoginUnexpectedResponseFailure, action: Action) {
        track(
            AnalyticsEvent.LOGIN_UNEXPECTED_RESPONSE_ACTION_TAPPED,
            failure,
            AnalyticsTracker.KEY_LOGIN_UNEXPECTED_RESPONSE_ACTION to action.trackingValue
        )
    }

    fun trackRetryResult(failure: LoginUnexpectedResponseFailure, isSuccess: Boolean) {
        track(
            AnalyticsEvent.LOGIN_UNEXPECTED_RESPONSE_RETRY_RESULT,
            failure,
            AnalyticsTracker.KEY_RESULT to if (isSuccess) VALUE_SUCCESS else VALUE_FAILURE
        )
    }

    private fun track(
        event: AnalyticsEvent,
        failure: LoginUnexpectedResponseFailure,
        vararg extraProperties: Pair<String, String>
    ) {
        analyticsTracker.track(
            event,
            mapOf(
                AnalyticsTracker.KEY_LOGIN_FLOW to failure.flow.trackingValue,
                AnalyticsTracker.KEY_STEP to failure.step.trackingValue,
                AnalyticsTracker.KEY_LOGIN_FAILURE_KIND to failure.response.kind.name.lowercase(Locale.ROOT)
            ) + extraProperties
        )
    }

    enum class Action(val trackingValue: String) {
        RETRY("retry"),
        CONTACT_SUPPORT("contact_support"),
        DISMISS("dismiss")
    }

    private companion object {
        const val VALUE_SUCCESS = "success"
        const val VALUE_FAILURE = "failure"
    }
}
