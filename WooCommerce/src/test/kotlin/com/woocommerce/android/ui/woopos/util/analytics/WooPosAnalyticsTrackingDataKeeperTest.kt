package com.woocommerce.android.ui.woopos.util.analytics

import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEventConstant.ReaderReadySignal
import org.assertj.core.api.Assertions.assertThat
import kotlin.test.Test

class WooPosAnalyticsTrackingDataKeeperTest {
    private val keeper = WooPosAnalyticsTrackingDataKeeper()

    @Test
    fun `given every stamp is set, when reset, then all stamps are cleared`() {
        // GIVEN
        keeper.startCustomerInteraction(timestamp = 1_000)
        keeper.onReaderConnectionChanged(isConnected = true, timestamp = 2_000)
        keeper.orderSyncSuccessTimestamp = 3_000
        keeper.readerReadyForPaymentTimestamp = 4_000
        keeper.readerReadyForAttemptTimestamp = 4_000
        keeper.readerReadySignal = ReaderReadySignal.INPUT_REQUESTED
        keeper.cardTappedTimestamp = 5_000
        keeper.checkoutButtonTapsCount = 2

        // WHEN
        keeper.reset()

        // THEN
        assertThat(keeper.interactionWithCustomerStartedTimestamp).isNull()
        assertThat(keeper.orderSyncSuccessTimestamp).isNull()
        assertThat(keeper.readerReadyForPaymentTimestamp).isNull()
        assertThat(keeper.readerReadyForAttemptTimestamp).isNull()
        assertThat(keeper.readerReadySignal).isNull()
        assertThat(keeper.cardTappedTimestamp).isNull()
        assertThat(keeper.checkoutButtonTapsCount).isZero()
        assertThat(keeper.readerConnectedAtCheckout).isNull()
        assertThat(keeper.readerConnectedTimestamp).isNull()
        assertThat(keeper.readerReadyTimingProperties).isEmpty()
    }

    @Test
    fun `given reader connected, when customer interaction starts, then reader is connected at checkout`() {
        // GIVEN
        keeper.onReaderConnectionChanged(isConnected = true, timestamp = 500)

        // WHEN
        keeper.startCustomerInteraction(timestamp = 1_000)

        // THEN
        assertThat(keeper.readerConnectedAtCheckout).isTrue()
        assertThat(keeper.readerReadyTimingProperties["reader_connected_at_checkout"]).isEqualTo("true")
        assertThat(keeper.millisecondsWaitingForReaderConnection).isNull()
    }

    @Test
    fun `given no reader connected, when customer interaction starts, then reader is not connected at checkout`() {
        // GIVEN
        keeper.onReaderConnectionChanged(isConnected = false, timestamp = 500)

        // WHEN
        keeper.startCustomerInteraction(timestamp = 1_000)

        // THEN
        assertThat(keeper.readerConnectedAtCheckout).isFalse()
        assertThat(keeper.readerReadyTimingProperties["reader_connected_at_checkout"]).isEqualTo("false")
    }

    @Test
    fun `given reader was connected, when a new interaction starts after reset, then the live connection is kept`() {
        // GIVEN
        keeper.onReaderConnectionChanged(isConnected = true, timestamp = 500)
        keeper.reset()

        // WHEN
        keeper.startCustomerInteraction(timestamp = 1_000)

        // THEN
        assertThat(keeper.readerConnectedAtCheckout).isTrue()
    }

    @Test
    fun `given interaction started without reader, when reader connects, then connection waiting is measured`() {
        // GIVEN
        keeper.onReaderConnectionChanged(isConnected = false, timestamp = 500)
        keeper.startCustomerInteraction(timestamp = 1_000)

        // WHEN
        keeper.onReaderConnectionChanged(isConnected = true, timestamp = 4_250)

        // THEN
        assertThat(keeper.millisecondsWaitingForReaderConnection).isEqualTo(3_250)
        assertThat(keeper.readerReadyTimingProperties["milliseconds_waiting_for_reader_connection"]).isEqualTo("3250")
    }

    @Test
    fun `given reader connected at start, when reader reconnects, then no connection waiting is measured`() {
        // GIVEN
        keeper.onReaderConnectionChanged(isConnected = true, timestamp = 500)
        keeper.startCustomerInteraction(timestamp = 1_000)

        // WHEN
        keeper.onReaderConnectionChanged(isConnected = false, timestamp = 2_000)
        keeper.onReaderConnectionChanged(isConnected = true, timestamp = 3_000)

        // THEN
        assertThat(keeper.readerConnectedTimestamp).isNull()
        assertThat(keeper.millisecondsWaitingForReaderConnection).isNull()
    }

    @Test
    fun `given reader connected during interaction, when it reconnects, then the first connection is kept`() {
        // GIVEN
        keeper.onReaderConnectionChanged(isConnected = false, timestamp = 500)
        keeper.startCustomerInteraction(timestamp = 1_000)
        keeper.onReaderConnectionChanged(isConnected = true, timestamp = 2_000)

        // WHEN
        keeper.onReaderConnectionChanged(isConnected = false, timestamp = 3_000)
        keeper.onReaderConnectionChanged(isConnected = true, timestamp = 4_000)

        // THEN
        assertThat(keeper.millisecondsWaitingForReaderConnection).isEqualTo(1_000)
    }

    @Test
    fun `given reader connected after order sync, when reader ready, then preparation starts at the connection`() {
        // GIVEN
        keeper.onReaderConnectionChanged(isConnected = false, timestamp = 500)
        keeper.startCustomerInteraction(timestamp = 1_000)
        keeper.orderSyncSuccessTimestamp = 2_000

        // WHEN
        keeper.onReaderConnectionChanged(isConnected = true, timestamp = 4_000)
        keeper.readerReadyForAttemptTimestamp = 7_000

        // THEN
        assertThat(keeper.millisecondsFromOrderSyncToReaderReady).isEqualTo(3_000)
    }

    @Test
    fun `given reader connected before order sync, when reader ready, then preparation starts at order sync`() {
        // GIVEN
        keeper.onReaderConnectionChanged(isConnected = false, timestamp = 500)
        keeper.startCustomerInteraction(timestamp = 1_000)
        keeper.onReaderConnectionChanged(isConnected = true, timestamp = 1_500)

        // WHEN
        keeper.orderSyncSuccessTimestamp = 3_000
        keeper.readerReadyForAttemptTimestamp = 5_000

        // THEN
        assertThat(keeper.millisecondsFromOrderSyncToReaderReady).isEqualTo(2_000)
        assertThat(keeper.readerReadyTimingProperties["milliseconds_from_order_sync_to_reader_ready"]).isEqualTo("2000")
    }

    @Test
    fun `given no reader ready stamp, when building properties, then preparation and signal are omitted`() {
        // GIVEN
        keeper.startCustomerInteraction(timestamp = 1_000)
        keeper.orderSyncSuccessTimestamp = 2_000

        // THEN
        assertThat(keeper.readerReadyTimingProperties)
            .doesNotContainKeys("milliseconds_from_order_sync_to_reader_ready", "reader_ready_signal")
    }

    @Test
    fun `given no status yet, when interaction starts and first status arrives, then connection is unknown`() {
        // WHEN
        keeper.startCustomerInteraction(timestamp = 1_000)
        keeper.onReaderConnectionChanged(isConnected = true, timestamp = 1_500)

        // THEN
        assertThat(keeper.readerConnectedAtCheckout).isNull()
        assertThat(keeper.readerConnectedTimestamp).isNull()
        assertThat(keeper.readerReadyTimingProperties)
            .doesNotContainKeys("reader_connected_at_checkout", "milliseconds_waiting_for_reader_connection")
    }

    @Test
    fun `given reader ready in previous attempt, when new attempt starts, then preparation and signal clear`() {
        // GIVEN
        keeper.startCustomerInteraction(timestamp = 1_000)
        keeper.orderSyncSuccessTimestamp = 2_000
        keeper.readerReadyForPaymentTimestamp = 3_000
        keeper.readerReadyForAttemptTimestamp = 3_000
        keeper.readerReadySignal = ReaderReadySignal.INPUT_REQUESTED

        // WHEN
        keeper.startPaymentAttempt()

        // THEN
        assertThat(keeper.millisecondsFromOrderSyncToReaderReady).isNull()
        assertThat(keeper.readerReadyTimingProperties)
            .doesNotContainKeys("milliseconds_from_order_sync_to_reader_ready", "reader_ready_signal")
        assertThat(keeper.readerReadyForPaymentTimestamp).isEqualTo(3_000)
    }

    @Test
    fun `given order synced after the reader was ready, when building properties, then preparation is omitted`() {
        // GIVEN
        keeper.startCustomerInteraction(timestamp = 1_000)
        keeper.readerReadyForAttemptTimestamp = 2_000

        // WHEN
        keeper.orderSyncSuccessTimestamp = 3_000

        // THEN
        assertThat(keeper.millisecondsFromOrderSyncToReaderReady).isNull()
    }
}
