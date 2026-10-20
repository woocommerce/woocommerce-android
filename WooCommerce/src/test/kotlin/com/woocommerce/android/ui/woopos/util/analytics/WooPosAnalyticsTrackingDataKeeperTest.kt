package com.woocommerce.android.ui.woopos.util.analytics

import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEventConstant.ReaderReadySignal
import org.assertj.core.api.Assertions.assertThat
import kotlin.test.Test

class WooPosAnalyticsTrackingDataKeeperTest {
    private val keeper = WooPosAnalyticsTrackingDataKeeper()

    @Test
    fun `given every stamp is set, when reset, then all stamps are cleared`() {
        // GIVEN
        keeper.interactionWithCustomerStartedTimestamp = 1_000
        keeper.orderSyncSuccessTimestamp = 2_000
        keeper.startPaymentAttempt(timestamp = 2_500)
        markReaderReady(at = 3_000)
        keeper.cardTappedTimestamp = 4_000
        keeper.checkoutButtonTapsCount = 2

        // WHEN
        keeper.reset()

        // THEN
        assertThat(keeper.interactionWithCustomerStartedTimestamp).isNull()
        assertThat(keeper.orderSyncSuccessTimestamp).isNull()
        assertThat(keeper.readerReadyForPaymentTimestamp).isNull()
        assertThat(keeper.paymentAttemptStartedTimestamp).isNull()
        assertThat(keeper.cardTappedTimestamp).isNull()
        assertThat(keeper.checkoutButtonTapsCount).isZero()
        assertThat(keeper.readerReadyTimingProperties).isEmpty()
    }

    @Test
    fun `given reader ready, when building properties, then preparation runs from order sync and the signal is set`() {
        // GIVEN
        keeper.orderSyncSuccessTimestamp = 2_000

        // WHEN
        markReaderReady(at = 3_500)

        // THEN
        assertThat(keeper.readerReadyTimingProperties).containsExactlyInAnyOrderEntriesOf(
            mapOf(
                "milliseconds_from_order_sync_to_reader_ready" to "1500",
                "reader_ready_signal" to "input_requested",
            )
        )
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

    private fun markReaderReady(at: Long) {
        keeper.readerReadyForPaymentTimestamp = at
        keeper.readerReadyForAttemptTimestamp = at
        keeper.readerReadySignal = ReaderReadySignal.INPUT_REQUESTED
    }
}
