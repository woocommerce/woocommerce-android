package com.woocommerce.android.ui.login.unexpectedresponse

import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.ui.login.unexpectedresponse.LoginUnexpectedResponseTracker.Action
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.wordpress.android.fluxc.network.UnexpectedStoreResponse
import org.wordpress.android.fluxc.network.UnexpectedStoreResponseKind

class LoginUnexpectedResponseTrackerTest {
    private val analyticsTracker: AnalyticsTrackerWrapper = mock()
    private val tracker = LoginUnexpectedResponseTracker(analyticsTracker)

    @Test
    fun `when the error is shown, then track its flow, step and kind`() {
        // WHEN
        tracker.trackErrorShown(FAILURE)

        // THEN
        verify(analyticsTracker).track(AnalyticsEvent.LOGIN_UNEXPECTED_RESPONSE_ERROR_SHOWN, FAILURE_PROPERTIES)
    }

    @Test
    fun `when an action is tapped, then track the action with the failure`() {
        // WHEN
        tracker.trackActionTapped(FAILURE, Action.CONTACT_SUPPORT)

        // THEN
        verify(analyticsTracker).track(
            AnalyticsEvent.LOGIN_UNEXPECTED_RESPONSE_ACTION_TAPPED,
            FAILURE_PROPERTIES + ("action" to "contact_support")
        )
    }

    @Test
    fun `when a retry finishes, then track its result with the failure`() {
        // WHEN
        tracker.trackRetryResult(FAILURE, isSuccess = false)

        // THEN
        verify(analyticsTracker).track(
            AnalyticsEvent.LOGIN_UNEXPECTED_RESPONSE_RETRY_RESULT,
            FAILURE_PROPERTIES + ("result" to "failure")
        )
    }

    private companion object {
        val FAILURE = LoginUnexpectedResponseFailure(
            flow = LoginUnexpectedResponseFailure.Flow.SITE_CREDENTIALS,
            step = LoginUnexpectedResponseFailure.Step.NONCE_RETRIEVAL,
            response = UnexpectedStoreResponse(
                kind = UnexpectedStoreResponseKind.UNACCEPTABLE_STATUS_CODE,
                statusCode = 400,
                contentType = "text/html",
                requestType = "GET /wp-admin/admin-ajax.php",
                excerpt = "0"
            )
        )
        val FAILURE_PROPERTIES = mapOf(
            "login_flow" to "site_credentials",
            "step" to "nonce_retrieval",
            "failure_kind" to "unacceptable_status_code"
        )
    }
}
