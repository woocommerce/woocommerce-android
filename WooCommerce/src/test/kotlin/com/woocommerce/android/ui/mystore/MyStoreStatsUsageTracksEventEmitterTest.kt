package com.woocommerce.android.ui.mystore

import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyNoInteractions
import org.wordpress.android.util.DateTimeUtils
import java.util.*

class MyStoreStatsUsageTracksEventEmitterTest {

    private val analyticsTrackerWrapper = mock<AnalyticsTrackerWrapper>()
    private val usageTracksEventEmitter = MyStoreStatsUsageTracksEventEmitter(analyticsTrackerWrapper)

//    @Before
//    fun setup() {
//
//    }

    @Test
    fun `test it will emit an event when the time and interaction thresholds are reached`() {
        // Given
        usageTracksEventEmitter.interacted(
            "2021-11-23T00:00:00Z",
            "2021-11-23T00:00:01Z",
            "2021-11-23T00:00:02Z",
            "2021-11-23T00:00:10Z",
        )

        verifyNoInteractions(analyticsTrackerWrapper)

        // When

        // Then

    }
}

private fun MyStoreStatsUsageTracksEventEmitter.interacted(vararg dates: String) {
    dates.forEach {
        val date = DateTimeUtils.dateUTCFromIso8601(it)
        interacted(at = date)
    }
}
