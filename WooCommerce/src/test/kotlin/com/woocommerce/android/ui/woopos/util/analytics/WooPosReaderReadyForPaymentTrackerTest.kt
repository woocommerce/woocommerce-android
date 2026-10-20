package com.woocommerce.android.ui.woopos.util.analytics

import com.woocommerce.android.ui.payments.tracking.CardReaderTrackingInfoProvider
import com.woocommerce.android.ui.payments.tracking.TrackingInfo
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEventConstant.ReaderReadySignal
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
        tracker.track(ReaderReadySignal.INPUT_REQUESTED)

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
        tracker.track(ReaderReadySignal.INPUT_REQUESTED)

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
        tracker.track(ReaderReadySignal.INPUT_REQUESTED)

        // THEN
        val event = argumentCaptor<WooPosAnalyticsEvent.Event.ReaderReadyForCardPayment>()
        verify(analyticsTracker).track(event.capture())
        assertThat(event.firstValue.properties).containsKey("waiting_time")
    }

    @Test
    fun `given order synced one and a half seconds ago, when tracked, then the waiting time keeps the fraction`() = runTest {
        // GIVEN
        whenever(trackingInfoProvider.trackingInfo).thenReturn(TrackingInfo(transport = "bluetooth"))
        analyticsData.orderSyncSuccessTimestamp = System.currentTimeMillis() - 1_500

        // WHEN
        tracker.track(ReaderReadySignal.INPUT_REQUESTED)

        // THEN
        val event = argumentCaptor<WooPosAnalyticsEvent.Event.ReaderReadyForCardPayment>()
        verify(analyticsTracker).track(event.capture())
        val waitingTime = event.firstValue.properties["waiting_time"] as String
        assertThat(waitingTime.toDouble()).isGreaterThanOrEqualTo(1.5)
    }

    @Test
    fun `given reader connected during the interaction, when tracked, then the event splits connection waiting from preparation`() =
        runTest {
            // GIVEN
            whenever(trackingInfoProvider.trackingInfo).thenReturn(TrackingInfo(transport = "bluetooth"))
            val now = System.currentTimeMillis()
            analyticsData.startCustomerInteraction(timestamp = now - 10_000)
            analyticsData.orderSyncSuccessTimestamp = now - 9_000
            analyticsData.onReaderConnectionChanged(isConnected = true, timestamp = now - 8_000)

            // WHEN
            tracker.track(ReaderReadySignal.INPUT_REQUESTED)

            // THEN
            val event = argumentCaptor<WooPosAnalyticsEvent.Event.ReaderReadyForCardPayment>()
            verify(analyticsTracker).track(event.capture())
            val properties = event.firstValue.properties
            assertThat(properties["reader_connected_at_checkout"]).isEqualTo("false")
            assertThat(properties["milliseconds_waiting_for_reader_connection"]).isEqualTo("2000")
            assertThat((properties["milliseconds_from_order_sync_to_reader_ready"] as String).toLong())
                .isGreaterThanOrEqualTo(8_000)
            assertThat(properties["reader_ready_signal"]).isEqualTo("input_requested")
        }

    @Test
    fun `given Tap to Pay, when tracked at collection start, then the event and keeper carry the collection_started signal`() =
        runTest {
            // GIVEN
            whenever(trackingInfoProvider.trackingInfo).thenReturn(TrackingInfo(transport = "built_in"))
            analyticsData.startCustomerInteraction(timestamp = System.currentTimeMillis())

            // WHEN
            tracker.track(ReaderReadySignal.COLLECTION_STARTED)

            // THEN
            val event = argumentCaptor<WooPosAnalyticsEvent.Event.ReaderReadyForCardPayment>()
            verify(analyticsTracker).track(event.capture())
            assertThat(event.firstValue.properties["reader_ready_signal"]).isEqualTo("collection_started")
            assertThat(analyticsData.readerReadySignal).isEqualTo(ReaderReadySignal.COLLECTION_STARTED)
            assertThat(analyticsData.readerReadyForPaymentTimestamp).isNotNull()
        }
}
