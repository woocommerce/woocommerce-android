package com.woocommerce.android.ui.woopos.util.analytics

import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.analytics.IAnalyticsEvent
import kotlinx.coroutines.test.runTest
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import kotlin.test.Test
import kotlin.test.assertFails

class WooPosAnalyticsTrackerTest {
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper = mock()
    private val commonPropertiesProvider: WooPosAnalyticsCommonPropertiesProvider = mock()

    val tracker = WooPosAnalyticsTracker(
        analyticsTrackerWrapper,
        commonPropertiesProvider,
    )

    @Test
    fun `given an event, when track is called, then it should track the event via wrapper`() = runTest {
        // GIVEN
        val event = WooPosAnalytics.Event.Test

        // WHEN
        tracker.track(event)

        // THEN
        verify(analyticsTrackerWrapper).track(
            event,
            event.properties
        )
    }

    @Test
    fun `given an err, when track is called, then it should track the error via wrapper`() = runTest {
        // GIVEN
        val error = WooPosAnalytics.Error.Test(
            errorContext = Any::class,
            errorType = "test",
            errorDescription = "test",
        )

        // WHEN
        tracker.track(error)

        // THEN
        verify(analyticsTrackerWrapper).track(
            error,
            error.properties,
            error.errorContext.simpleName,
            error.errorType,
            error.errorDescription
        )
    }

    @Test
    fun `given an non woopos event, when track is called, then it throw an exception`() = runTest {
        // GIVEN
        val event = object : IAnalyticsEvent {
            override val name: String = "test"
            override val siteless: Boolean = false
            override val isPosEvent: Boolean = false
        }

        // WHEN && THEN
        assertFails { tracker.track(event) }
    }
}
