package com.woocommerce.android.ui.woopos.util.analytics

import com.woocommerce.android.ui.payments.tracking.CardReaderTrackingInfoProvider
import com.woocommerce.android.ui.payments.tracking.TrackingInfo
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.Test

class WooPosReaderReadyForPaymentTrackerTest {
    private val analyticsTracker: WooPosAnalyticsTracker = mock()
    private val analyticsData = WooPosAnalyticsTrackingDataKeeper()
    private val trackingInfoProvider: CardReaderTrackingInfoProvider = mock()

    private val tracker = WooPosReaderReadyForPaymentTracker(
        analyticsTracker = analyticsTracker,
        analyticsData = analyticsData,
        trackingInfoProvider = trackingInfoProvider,
    )

    @Test
    fun `given a remote reader connection, when tracked, then the event carries the transport`() = runTest {
        // GIVEN
        whenever(trackingInfoProvider.trackingInfo).thenReturn(TrackingInfo(transport = "wifi_lan"))

        // WHEN
        tracker.track()

        // THEN
        val event = argumentCaptor<WooPosAnalyticsEvent.Event.ReaderReadyForCardPayment>()
        verify(analyticsTracker).track(event.capture())
        assertThat(event.firstValue.properties["transport"]).isEqualTo("wifi_lan")
    }

    @Test
    fun `given no known transport, when tracked, then the event carries no transport`() = runTest {
        // GIVEN
        whenever(trackingInfoProvider.trackingInfo).thenReturn(TrackingInfo(transport = null))

        // WHEN
        tracker.track()

        // THEN
        val event = argumentCaptor<WooPosAnalyticsEvent.Event.ReaderReadyForCardPayment>()
        verify(analyticsTracker).track(event.capture())
        assertThat(event.firstValue.properties).doesNotContainKey("transport")
    }

    @Test
    fun `given a synced order, when tracked, then the event carries the waiting time`() = runTest {
        // GIVEN
        whenever(trackingInfoProvider.trackingInfo).thenReturn(TrackingInfo(transport = "bluetooth"))
        analyticsData.orderSyncSuccessTimestamp = System.currentTimeMillis()

        // WHEN
        tracker.track()

        // THEN
        val event = argumentCaptor<WooPosAnalyticsEvent.Event.ReaderReadyForCardPayment>()
        verify(analyticsTracker).track(event.capture())
        assertThat(event.firstValue.properties).containsKey("waiting_time")
    }
}
