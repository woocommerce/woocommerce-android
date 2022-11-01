package com.woocommerce.android.ui.jitm

import com.woocommerce.android.analytics.AnalyticsEvent.JITM_FETCH_FAILURE
import com.woocommerce.android.analytics.AnalyticsEvent.JITM_FETCH_SUCCESS
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_JITM
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_JITM_COUNT
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_SOURCE
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.ui.JitmTracker
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
}
