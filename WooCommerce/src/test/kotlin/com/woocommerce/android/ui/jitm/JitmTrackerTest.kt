package com.woocommerce.android.ui.jitm

import com.woocommerce.android.analytics.AnalyticsEvent.JITM_FETCH_FAILURE
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
}
