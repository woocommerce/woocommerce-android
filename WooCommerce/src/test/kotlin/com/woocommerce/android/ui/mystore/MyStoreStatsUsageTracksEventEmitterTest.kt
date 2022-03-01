package com.woocommerce.android.ui.mystore

import com.woocommerce.android.analytics.AnalyticsTracker.Stat.USED_ANALYTICS
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import org.junit.Test
import org.mockito.kotlin.*
import org.wordpress.android.util.DateTimeUtils

class MyStoreStatsUsageTracksEventEmitterTest {
    private val analyticsTrackerWrapper = mock<AnalyticsTrackerWrapper>()
    private val usageTracksEventEmitter = MyStoreStatsUsageTracksEventEmitter(analyticsTrackerWrapper)

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
        usageTracksEventEmitter.interacted("2021-11-23T00:00:11Z")

        // Then
        verify(analyticsTrackerWrapper, times(1)).track(USED_ANALYTICS)
    }

    @Test
    fun `test it will not emit an event if the interaction threshold is not reached`() {
        // Given
        usageTracksEventEmitter.interacted(
            "2021-11-23T00:00:00Z",
            "2021-11-23T00:00:01Z",
            "2021-11-23T00:00:02Z",
        )

        // When
        usageTracksEventEmitter.interacted("2021-11-23T00:00:11Z")

        // Then
        verifyNoInteractions(analyticsTrackerWrapper)
    }

    @Test
    fun `test it will not emit an event if the time threshold is not reached`() {
        // Given
        usageTracksEventEmitter.interacted(
            "2021-11-23T00:00:00Z",
            "2021-11-23T00:00:01Z",
            "2021-11-23T00:00:02Z",
            "2021-11-23T00:00:03Z",
        )

        // When
        usageTracksEventEmitter.interacted("2021-11-23T00:00:04Z")

        // Then
        verifyNoInteractions(analyticsTrackerWrapper)
    }

    @Test
    fun `test it will not emit an event when the user idled`() {
        // Given
        usageTracksEventEmitter.interacted(
            "2021-11-23T00:00:00Z",
            "2021-11-23T00:00:01Z",
            "2021-11-23T00:00:02Z",
            "2021-11-23T00:00:04Z",
        )

        // When
        // 20 seconds after the last one
        usageTracksEventEmitter.interacted("2021-11-23T00:00:24Z")

        // Then
        verifyNoInteractions(analyticsTrackerWrapper)
    }

    @Test
    fun `test it can still emit an event later after idling`() {
        // Given
        usageTracksEventEmitter.interacted(
            "2021-11-23T00:00:00Z",
            "2021-11-23T00:00:01Z",
            "2021-11-23T00:00:02Z",
            "2021-11-23T00:00:04Z",
            "2021-11-23T00:00:24Z", // idled
        )

        verifyNoInteractions(analyticsTrackerWrapper)

        // When
        usageTracksEventEmitter.interacted(
            "2021-11-23T00:01:00Z",
            "2021-11-23T00:01:01Z",
            "2021-11-23T00:01:02Z",
            "2021-11-23T00:01:04Z",
            "2021-11-23T00:01:10Z",
        )

        // Then
        verify(analyticsTrackerWrapper, times(1)).track(USED_ANALYTICS)
    }

    @Test
    fun `test it will not emit an event right away after an event was previously emitted`() {
        // Given
        usageTracksEventEmitter.interacted(
            "2021-11-23T00:00:00Z",
            "2021-11-23T00:00:01Z",
            "2021-11-23T00:00:02Z",
            "2021-11-23T00:00:10Z",
            "2021-11-23T00:00:11Z", // event triggered here
        )

        verify(analyticsTrackerWrapper, times(1)).track(USED_ANALYTICS)

        // When
        usageTracksEventEmitter.interacted("2021-11-23T00:00:12Z")

        // Then
        verifyNoMoreInteractions(analyticsTrackerWrapper)
    }

    @Test
    fun `test it will emit another event if the threshold is reached again`() {
        // Given
        usageTracksEventEmitter.interacted(
            "2021-11-23T00:00:00Z",
            "2021-11-23T00:00:02Z",
            "2021-11-23T00:00:04Z",
            "2021-11-23T00:00:06Z",
            "2021-11-23T00:00:10Z", // event triggered here
            "2021-11-23T00:00:12Z",
            "2021-11-23T00:00:14Z",
            "2021-11-23T00:00:16Z",
            "2021-11-23T00:00:18Z",
        )

        // Only 1 event because the second set of interactions have not reached the threshold yet
        verify(analyticsTrackerWrapper, times(1)).track(USED_ANALYTICS)

        reset(analyticsTrackerWrapper)

        // When
        usageTracksEventEmitter.interacted("2021-11-23T00:00:22Z")

        // Then
        verify(analyticsTrackerWrapper, times(1)).track(USED_ANALYTICS)
    }
}

private fun MyStoreStatsUsageTracksEventEmitter.interacted(vararg dates: String) {
    dates.forEach {
        val date = DateTimeUtils.dateUTCFromIso8601(it)
        interacted(date)
    }
}
