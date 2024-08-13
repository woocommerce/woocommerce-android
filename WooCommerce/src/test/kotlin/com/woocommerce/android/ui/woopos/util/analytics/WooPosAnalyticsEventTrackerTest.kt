package com.woocommerce.android.ui.woopos.util.analytics

import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.analytics.IAnalyticsEvent
import kotlinx.coroutines.test.runTest
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.Test
import kotlin.test.assertFails

class WooPosAnalyticsEventTrackerTest {
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper = mock()
    private val commonPropertiesProvider: WooPosAnalyticsCommonPropertiesProvider = mock()

    val tracker = WooPosAnalyticsTracker(
        analyticsTrackerWrapper,
        commonPropertiesProvider,
    )

    @Test
    fun `given an event, when track is called, then it should track the event via wrapper`() = runTest {
        // GIVEN
        val event = WooPosAnalyticsEvent.Event.Test

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
        val error = WooPosAnalyticsEvent.Error.Test(
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
    fun `given an event and common properties, when track is called, then it should track the event with common properties`() = runTest {
        // GIVEN
        val event = WooPosAnalyticsEvent.Event.Test
        val commonProperties = mapOf("test" to "test")
        whenever(commonPropertiesProvider.commonProperties).thenReturn(commonProperties)

        // WHEN
        tracker.track(event)

        // THEN
        verify(analyticsTrackerWrapper).track(
            event,
            event.properties + commonProperties
        )
    }

    @Test
    fun `given an error and common properties, when track is called, then it should track the event with common properties`() = runTest {
        // GIVEN
        val error = WooPosAnalyticsEvent.Error.Test(
            errorContext = Any::class,
            errorType = "test",
            errorDescription = "test",
        )
        val commonProperties = mapOf("test" to "test")
        whenever(commonPropertiesProvider.commonProperties).thenReturn(commonProperties)

        // WHEN
        tracker.track(error)

        // THEN
        verify(analyticsTrackerWrapper).track(
            error,
            error.properties + commonProperties,
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
