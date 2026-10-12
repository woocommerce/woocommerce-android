package com.woocommerce.android.ui.jitm

import com.woocommerce.android.analytics.AnalyticsEvent.JITM_CTA_TAPPED
import com.woocommerce.android.analytics.AnalyticsEvent.JITM_DISMISS_FAILURE
import com.woocommerce.android.analytics.AnalyticsEvent.JITM_DISMISS_SUCCESS
import com.woocommerce.android.analytics.AnalyticsEvent.JITM_DISMISS_TAPPED
import com.woocommerce.android.analytics.AnalyticsEvent.JITM_DISPLAYED
import com.woocommerce.android.analytics.AnalyticsEvent.JITM_FETCH_FAILURE
import com.woocommerce.android.analytics.AnalyticsEvent.JITM_FETCH_SUCCESS
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.JITM_GROUP
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.JITM_ID
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_ERROR_CODE
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_ERROR_DESC
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_JITM
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_JITM_COUNT
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_SOURCE
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType

@ExperimentalCoroutinesApi
class JitmTrackerTest : BaseUnitTest() {
    private val trackerWrapper: AnalyticsTrackerWrapper = mock()

    private val jitmTracker = JitmTracker(trackerWrapper)

    @Test
    fun `when track jitm failure invoked, then JITM_FETCH_FAILURE tracked`() {
        testBlocking {
            jitmTracker.trackJitmFetchFailure(UTM_SOURCE, WooErrorType.GENERIC_ERROR, "debug message")

            verify(trackerWrapper).track(
                eq(JITM_FETCH_FAILURE),
                any(),
                anyString(),
                anyString(),
                anyString(),
            )
        }
    }

    @Test
    fun `when track jitm failure invoked, then JITM_FETCH_FAILURE tracked with correct properties`() {
        testBlocking {
            jitmTracker.trackJitmFetchFailure(UTM_SOURCE, WooErrorType.GENERIC_ERROR, "debug message")

            verify(trackerWrapper).track(
                JITM_FETCH_FAILURE,
                mapOf(
                    KEY_SOURCE to UTM_SOURCE
                ),
                "JitmTracker",
                WooErrorType.GENERIC_ERROR.name,
                "debug message",
            )
        }
    }

    @Test
    fun `when track jitm success invoked, then JITM_FETCH_SUCCESS tracked`() {
        testBlocking {
            jitmTracker.trackJitmFetchSuccess(
                UTM_SOURCE,
                "12345",
                1
            )

            verify(trackerWrapper).track(
                eq(JITM_FETCH_SUCCESS),
                any(),
            )
        }
    }

    @Test
    fun `when track jitm success invoked, then JITM_FETCH_SUCCESS tracked with correct properties`() {
        testBlocking {
            jitmTracker.trackJitmFetchSuccess(
                UTM_SOURCE,
                "12345",
                1
            )

            verify(trackerWrapper).track(
                JITM_FETCH_SUCCESS,
                mapOf(
                    KEY_SOURCE to UTM_SOURCE,
                    KEY_JITM to "12345",
                    KEY_JITM_COUNT to 1
                ),
            )
        }
    }

    @Test
    fun `when track jitm displayed invoked, then JITM_FETCH_DISPLAYED tracked`() {
        testBlocking {
            jitmTracker.trackJitmDisplayed(
                UTM_SOURCE,
                "12345",
                ""
            )

            verify(trackerWrapper).track(
                eq(JITM_DISPLAYED),
                any(),
            )
        }
    }

    @Test
    fun `when track jitm displayed invoked, then JITM_FETCH_DISPLAYED tracked with correct properties`() {
        testBlocking {
            jitmTracker.trackJitmDisplayed(
                UTM_SOURCE,
                "12345",
                "test_jitm_group"
            )

            verify(trackerWrapper).track(
                JITM_DISPLAYED,
                mapOf(
                    KEY_SOURCE to UTM_SOURCE,
                    JITM_ID to "12345",
                    JITM_GROUP to "test_jitm_group"
                ),
            )
        }
    }

    @Test
    fun `when track jitm cta clicked invoked, then JITM_CTA_TAPPED tracked`() {
        testBlocking {
            jitmTracker.trackJitmCtaTapped(
                UTM_SOURCE,
                "12345",
                ""
            )

            verify(trackerWrapper).track(
                eq(JITM_CTA_TAPPED),
                any(),
            )
        }
    }

    @Test
    fun `when track jitm cta clicked invoked, then JITM_CTA_TAPPED tracked with correct properties`() {
        testBlocking {
            jitmTracker.trackJitmCtaTapped(
                UTM_SOURCE,
                "12345",
                "test_jitm_group"
            )

            verify(trackerWrapper).track(
                JITM_CTA_TAPPED,
                mapOf(
                    KEY_SOURCE to UTM_SOURCE,
                    JITM_ID to "12345",
                    JITM_GROUP to "test_jitm_group"
                ),
            )
        }
    }

    @Test
    fun `when track jitm dismiss clicked invoked, then JITM_DISMISS_TAPPED tracked`() {
        testBlocking {
            jitmTracker.trackJitmDismissTapped(
                UTM_SOURCE,
                "12345",
                ""
            )

            verify(trackerWrapper).track(
                eq(JITM_DISMISS_TAPPED),
                any(),
            )
        }
    }

    @Test
    fun `when track jitm dismiss clicked invoked, then JITM_DISMISS_TAPPED tracked with correct properties`() {
        testBlocking {
            jitmTracker.trackJitmDismissTapped(
                UTM_SOURCE,
                "12345",
                "test_jitm_group"
            )

            verify(trackerWrapper).track(
                JITM_DISMISS_TAPPED,
                mapOf(
                    KEY_SOURCE to UTM_SOURCE,
                    JITM_ID to "12345",
                    JITM_GROUP to "test_jitm_group"
                )
            )
        }
    }

    @Test
    fun `when track jitm dismiss success invoked, then JITM_DISMISS_SUCCESS tracked`() {
        testBlocking {
            jitmTracker.trackJitmDismissSuccess(
                UTM_SOURCE,
                "12345",
                ""
            )

            verify(trackerWrapper).track(
                eq(JITM_DISMISS_SUCCESS),
                any(),
            )
        }
    }

    @Test
    fun `when track jitm dismiss success invoked, then JITM_DISMISS_SUCCESS tracked with correct properties`() {
        testBlocking {
            jitmTracker.trackJitmDismissSuccess(
                UTM_SOURCE,
                "12345",
                "test_jitm_group"
            )

            verify(trackerWrapper).track(
                JITM_DISMISS_SUCCESS,
                mapOf(
                    KEY_SOURCE to UTM_SOURCE,
                    JITM_ID to "12345",
                    JITM_GROUP to "test_jitm_group"
                )
            )
        }
    }

    @Test
    fun `given error with api error code, when track jitm dismiss failure invoked, then api error code is tracked`() {
        testBlocking {
            jitmTracker.trackJitmDismissFailure(
                UTM_SOURCE,
                "12345",
                "test_jitm_group",
                WooError(
                    type = WooErrorType.API_ERROR,
                    original = GenericErrorType.SERVER_ERROR,
                    message = "test error",
                    apiErrorCode = "rest_invalid_signature"
                )
            )

            verify(trackerWrapper).track(
                JITM_DISMISS_FAILURE,
                mapOf(
                    KEY_SOURCE to UTM_SOURCE,
                    JITM_ID to "12345",
                    JITM_GROUP to "test_jitm_group",
                    KEY_ERROR_CODE to "rest_invalid_signature",
                    KEY_ERROR_DESC to "test error",
                )
            )
        }
    }

    @Test
    fun `given error without api error code, when track jitm dismiss failure invoked, then error type is tracked`() {
        testBlocking {
            jitmTracker.trackJitmDismissFailure(
                UTM_SOURCE,
                "12345",
                "test_jitm_group",
                WooError(
                    type = WooErrorType.TIMEOUT,
                    original = GenericErrorType.TIMEOUT,
                    message = "timeout"
                )
            )

            verify(trackerWrapper).track(
                JITM_DISMISS_FAILURE,
                mapOf(
                    KEY_SOURCE to UTM_SOURCE,
                    JITM_ID to "12345",
                    JITM_GROUP to "test_jitm_group",
                    KEY_ERROR_CODE to WooErrorType.TIMEOUT.name,
                    KEY_ERROR_DESC to "timeout",
                )
            )
        }
    }

    @Test
    fun `given error with empty api error code and message, when track jitm dismiss failure invoked, then only error type is tracked`() {
        testBlocking {
            jitmTracker.trackJitmDismissFailure(
                UTM_SOURCE,
                "12345",
                "test_jitm_group",
                WooError(
                    type = WooErrorType.NO_CONNECTION,
                    original = GenericErrorType.NO_CONNECTION,
                    message = "",
                    apiErrorCode = ""
                )
            )

            verify(trackerWrapper).track(
                JITM_DISMISS_FAILURE,
                mapOf(
                    KEY_SOURCE to UTM_SOURCE,
                    JITM_ID to "12345",
                    JITM_GROUP to "test_jitm_group",
                    KEY_ERROR_CODE to WooErrorType.NO_CONNECTION.name,
                )
            )
        }
    }

    @Test
    fun `given no error, when track jitm dismiss failure invoked, then generic error code is tracked`() {
        testBlocking {
            jitmTracker.trackJitmDismissFailure(
                UTM_SOURCE,
                "12345",
                "test_jitm_group",
                null
            )

            verify(trackerWrapper).track(
                JITM_DISMISS_FAILURE,
                mapOf(
                    KEY_SOURCE to UTM_SOURCE,
                    JITM_ID to "12345",
                    JITM_GROUP to "test_jitm_group",
                    KEY_ERROR_CODE to WooErrorType.GENERIC_ERROR.name,
                )
            )
        }
    }

    @Test
    fun `given error with api error code and blank message, when track jitm dismiss failure invoked, then only error code is tracked`() {
        testBlocking {
            jitmTracker.trackJitmDismissFailure(
                UTM_SOURCE,
                "12345",
                "test_jitm_group",
                WooError(
                    type = WooErrorType.API_ERROR,
                    original = GenericErrorType.SERVER_ERROR,
                    message = " ",
                    apiErrorCode = "rest_invalid_signature"
                )
            )

            verify(trackerWrapper).track(
                JITM_DISMISS_FAILURE,
                mapOf(
                    KEY_SOURCE to UTM_SOURCE,
                    JITM_ID to "12345",
                    JITM_GROUP to "test_jitm_group",
                    KEY_ERROR_CODE to "rest_invalid_signature",
                )
            )
        }
    }

    companion object {
        private const val UTM_SOURCE = "my_store"
    }
}
