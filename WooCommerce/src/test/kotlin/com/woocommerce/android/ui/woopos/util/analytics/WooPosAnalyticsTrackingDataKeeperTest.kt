package com.woocommerce.android.ui.woopos.util.analytics

import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEventConstant.ReaderReadySignal
import org.assertj.core.api.Assertions.assertThat
import org.mockito.kotlin.mock
import kotlin.test.Test

class WooPosAnalyticsTrackingDataKeeperTest {
    private val keeper = WooPosAnalyticsTrackingDataKeeper(mock())

    @Test
    fun `given every stamp is set, when a new interaction starts, then stamps clear and the live connection is kept`() {
        // GIVEN
        disconnect(at = 500)
        keeper.startCustomerInteraction(timestamp = 1_000)
        connect(at = 2_000)
        keeper.orderSyncSuccessTimestamp = 3_000
        keeper.startPaymentAttempt(timestamp = 3_500)
        markReaderReady(at = 4_000)
        keeper.cardTappedTimestamp = 5_000
        keeper.checkoutButtonTapsCount = 2

        // WHEN
        keeper.startCustomerInteraction(timestamp = 6_000)

        // THEN
        assertThat(keeper.orderSyncSuccessTimestamp).isNull()
        assertThat(keeper.readerReadyForPaymentTimestamp).isNull()
        assertThat(keeper.paymentAttemptStartedTimestamp).isNull()
        assertThat(keeper.cardTappedTimestamp).isNull()
        assertThat(keeper.checkoutButtonTapsCount).isZero()
        assertThat(keeper.readerConnectedTimestamp).isNull()
        assertThat(keeper.latestReaderConnectedTimestamp).isNull()
        assertThat(keeper.readerReadyTimingProperties).containsExactlyEntriesOf(
            mapOf("reader_connected_at_checkout" to "true")
        )
    }

    @Test
    fun `given reader connected at start, when it reconnects, then no connection waiting is measured`() {
        // GIVEN
        connect(at = 500)
        keeper.startCustomerInteraction(timestamp = 1_000)

        // WHEN
        disconnect(at = 2_000)
        connect(at = 3_000)

        // THEN
        assertThat(keeper.readerConnectedAtCheckout).isTrue()
        assertThat(keeper.millisecondsWaitingForReaderConnection).isNull()
    }

    @Test
    fun `given no reader at start, when it connects and reconnects, then waiting runs to the first connection`() {
        // GIVEN
        disconnect(at = 500)
        keeper.startCustomerInteraction(timestamp = 1_000)

        // WHEN
        connect(at = 4_250)
        disconnect(at = 5_000)
        connect(at = 6_000)

        // THEN
        assertThat(keeper.readerReadyTimingProperties)
            .containsEntry("reader_connected_at_checkout", "false")
            .containsEntry("milliseconds_waiting_for_reader_connection", "3250")
    }

    @Test
    fun `given reader connected after order sync, when reader ready, then preparation starts at the connection`() {
        // GIVEN
        disconnect(at = 500)
        keeper.startCustomerInteraction(timestamp = 1_000)
        keeper.orderSyncSuccessTimestamp = 2_000

        // WHEN
        connect(at = 4_000)
        markReaderReady(at = 7_000)

        // THEN
        assertThat(keeper.readerReadyTimingProperties)
            .containsEntry("milliseconds_from_order_sync_to_reader_ready", "3000")
            .containsEntry("reader_ready_signal", "input_requested")
    }

    @Test
    fun `given reader connected before order sync, when reader ready, then preparation starts at order sync`() {
        // GIVEN
        disconnect(at = 500)
        keeper.startCustomerInteraction(timestamp = 1_000)
        connect(at = 1_500)

        // WHEN
        keeper.orderSyncSuccessTimestamp = 3_000
        markReaderReady(at = 5_000)

        // THEN
        assertThat(keeper.millisecondsFromOrderSyncToReaderReady).isEqualTo(2_000)
    }

    @Test
    fun `given no status yet, when interaction starts and the first status is connected, then connection is unknown`() {
        // WHEN
        keeper.startCustomerInteraction(timestamp = 1_000)
        connect(at = 1_500)

        // THEN
        assertThat(keeper.readerConnectedAtCheckout).isNull()
        assertThat(keeper.readerConnectedTimestamp).isNull()
        assertThat(keeper.latestReaderConnectedTimestamp).isNull()
        assertThat(keeper.readerReadyTimingProperties).isEmpty()
    }

    @Test
    fun `given no status yet at start, when reader disconnects and reconnects, then only preparation uses it`() {
        // GIVEN
        keeper.startCustomerInteraction(timestamp = 1_000)
        keeper.orderSyncSuccessTimestamp = 2_000
        disconnect(at = 2_500)

        // WHEN
        connect(at = 4_000)
        markReaderReady(at = 5_000)

        // THEN
        assertThat(keeper.millisecondsFromOrderSyncToReaderReady).isEqualTo(1_000)
        assertThat(keeper.readerReadyTimingProperties)
            .doesNotContainKeys("reader_connected_at_checkout", "milliseconds_waiting_for_reader_connection")
    }

    @Test
    fun `given reader reconnects during the attempt, when reader ready, then preparation starts at the reconnect`() {
        // GIVEN
        connect(at = 500)
        keeper.startCustomerInteraction(timestamp = 1_000)
        keeper.orderSyncSuccessTimestamp = 2_000
        keeper.startPaymentAttempt(timestamp = 2_500)

        // WHEN
        disconnect(at = 3_000)
        connect(at = 5_000)
        markReaderReady(at = 6_000)

        // THEN
        assertThat(keeper.millisecondsFromOrderSyncToReaderReady).isEqualTo(1_000)
    }

    @Test
    fun `given reader ready, when it reconnects before the outcome, then preparation is kept until the next attempt`() {
        // GIVEN
        connect(at = 500)
        keeper.startCustomerInteraction(timestamp = 1_000)
        keeper.orderSyncSuccessTimestamp = 2_000
        keeper.startPaymentAttempt(timestamp = 2_500)
        markReaderReady(at = 3_000)

        // WHEN
        disconnect(at = 4_000)
        connect(at = 5_000)

        // THEN
        assertThat(keeper.millisecondsFromOrderSyncToReaderReady).isEqualTo(500)
        keeper.startPaymentAttempt(timestamp = 6_000)
        markReaderReady(at = 6_750)
        assertThat(keeper.millisecondsFromOrderSyncToReaderReady).isEqualTo(750)
    }

    @Test
    fun `given a declined attempt, when a retry starts and its reader is ready, then only the retry is measured`() {
        // GIVEN
        keeper.orderSyncSuccessTimestamp = 1_000
        keeper.startPaymentAttempt(timestamp = 1_000)
        markReaderReady(at = 2_000)

        // WHEN
        keeper.startPaymentAttempt(timestamp = 30_000)

        // THEN
        assertThat(keeper.readerReadyTimingProperties).isEmpty()
        assertThat(keeper.readerReadyForPaymentTimestamp).isEqualTo(2_000)
        markReaderReady(at = 32_000)
        assertThat(keeper.millisecondsFromOrderSyncToReaderReady).isEqualTo(2_000)
    }

    @Test
    fun `given order synced after the reader was ready, when building properties, then preparation is omitted`() {
        // GIVEN
        markReaderReady(at = 2_000)

        // WHEN
        keeper.orderSyncSuccessTimestamp = 3_000

        // THEN
        assertThat(keeper.millisecondsFromOrderSyncToReaderReady).isNull()
    }

    private fun connect(at: Long) = keeper.onReaderConnectionChanged(isConnected = true, timestamp = at)

    private fun disconnect(at: Long) = keeper.onReaderConnectionChanged(isConnected = false, timestamp = at)

    private fun markReaderReady(at: Long) {
        keeper.readerReadyForPaymentTimestamp = at
        keeper.readerReadyForAttemptTimestamp = at
        keeper.readerReadySignal = ReaderReadySignal.INPUT_REQUESTED
    }
}
