package com.woocommerce.android.ui.mystore

import com.woocommerce.android.analytics.AnalyticsEvent.USED_ANALYTICS
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.TestScope
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.util.DateTimeUtils

@ExperimentalCoroutinesApi
class MyStoreStatsUsageTracksEventEmitterTest : BaseUnitTest() {
    private val analyticsTrackerWrapper = mock<AnalyticsTrackerWrapper>()
    private val siteFlow = MutableSharedFlow<SiteModel>()
    private val selectedSite: SelectedSite = mock {
        on { observe() } doReturn siteFlow
    }
    private val usageTracksEventEmitter = MyStoreStatsUsageTracksEventEmitter(
        analyticsTrackerWrapper = analyticsTrackerWrapper,
        appCoroutineScope = TestScope(coroutinesTestRule.testDispatcher),
        selectedSite = selectedSite
    )

    @Test
    fun `given some interactions, when time and interaction thresholds are reached, it will emit an event`() {
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
    fun `given some interactions, when the site is changed, then it will not emit an event`() = testBlocking {
        // Given
        usageTracksEventEmitter.interacted(
            "2021-11-23T00:00:00Z",
            "2021-11-23T00:00:01Z",
            "2021-11-23T00:00:02Z",
            "2021-11-23T00:00:10Z",
        )

        verifyNoInteractions(analyticsTrackerWrapper)

        // When
        // This should cause a reset()
        siteFlow.emit(SiteModel())

        // Since reset() was called above, this will not cause an event to be triggered.
        usageTracksEventEmitter.interacted("2021-11-23T00:00:11Z")

        // Then
        verifyNoInteractions(analyticsTrackerWrapper)
    }

    @Test
    fun `given some interactions, when the interactions threshold is not reached, then it will not emit an event`() {
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
    fun `given some interactions, when the time threshold is not reached, then it will not emit an event`() {
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
    fun `given some interactions, when the user idled, then it will not emit an event`() {
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
    fun `given an idled user, when new interactions reach the thresholds, then it will emit an event`() {
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
    fun `given an event was emitted, when one interaction happens after, then it will not emit an event`() {
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
    fun `given an event was emitted, when the thresholds are reached again, then it will emit another event`() {
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
