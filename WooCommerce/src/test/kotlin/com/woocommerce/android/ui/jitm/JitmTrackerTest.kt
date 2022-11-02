package com.woocommerce.android.ui.jitm

import com.woocommerce.android.analytics.AnalyticsEvent.JITM_CTA_TAPPED
import com.woocommerce.android.analytics.AnalyticsEvent.JITM_DISMISS_FAILURE
import com.woocommerce.android.analytics.AnalyticsEvent.JITM_DISMISS_SUCCESS
import com.woocommerce.android.analytics.AnalyticsEvent.JITM_DISMISS_TAPPED
import com.woocommerce.android.analytics.AnalyticsEvent.JITM_DISPLAYED
import com.woocommerce.android.analytics.AnalyticsEvent.JITM_FETCH_FAILURE
import com.woocommerce.android.analytics.AnalyticsEvent.JITM_FETCH_SUCCESS
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.JITM_FEATURE_CLASS
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.JITM_ID
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_JITM
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_JITM_COUNT
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_SOURCE
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.ui.mystore.MyStoreViewModel.Companion.UTM_SOURCE
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
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
                "test_feature_class"
            )

            verify(trackerWrapper).track(
                JITM_DISPLAYED,
                mapOf(
                    KEY_SOURCE to UTM_SOURCE,
                    JITM_ID to "12345",
                    JITM_FEATURE_CLASS to "test_feature_class"
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
                "test_feature_class"
            )

            verify(trackerWrapper).track(
                JITM_CTA_TAPPED,
                mapOf(
                    KEY_SOURCE to UTM_SOURCE,
                    JITM_ID to "12345",
                    JITM_FEATURE_CLASS to "test_feature_class"
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
                "test_feature_class"
            )

            verify(trackerWrapper).track(
                JITM_DISMISS_TAPPED,
                mapOf(
                    KEY_SOURCE to UTM_SOURCE,
                    JITM_ID to "12345",
                    JITM_FEATURE_CLASS to "test_feature_class"
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
                "test_feature_class"
            )

            verify(trackerWrapper).track(
                JITM_DISMISS_SUCCESS,
                mapOf(
                    KEY_SOURCE to UTM_SOURCE,
                    JITM_ID to "12345",
                    JITM_FEATURE_CLASS to "test_feature_class"
                )
            )
        }
    }

    @Test
    fun `when track jitm dismiss failure invoked, then JITM_DISMISS_FAILURE tracked`() {
        testBlocking {
            jitmTracker.trackJitmDismissFailure(
                UTM_SOURCE,
                "12345",
                "",
                WooErrorType.GENERIC_ERROR,
                "test error"
            )

            verify(trackerWrapper).track(
                eq(JITM_DISMISS_FAILURE),
                any(),
                any(),
                any(),
                any(),
            )
        }
    }

    @Test
    fun `when track jitm dismiss failure invoked, then JITM_DISMISS_FAILURE tracked with correct properties`() {
        testBlocking {
            jitmTracker.trackJitmDismissFailure(
                UTM_SOURCE,
                "12345",
                "test_feature_class",
                WooErrorType.GENERIC_ERROR,
                "test error"
            )

            verify(trackerWrapper).track(
                JITM_DISMISS_FAILURE,
                mapOf(
                    KEY_SOURCE to UTM_SOURCE,
                    JITM_ID to "12345",
                    JITM_FEATURE_CLASS to "test_feature_class",
                ),
                errorContext = "JitmTracker",
                errorType = WooErrorType.GENERIC_ERROR.name,
                errorDescription = "test error",
            )
        }
    }
}
