package com.woocommerce.android.ui.woopos.util.analytics

import com.woocommerce.android.analytics.IAnalyticsEvent
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.PaymentFlowTrackerEvent.CardPresentCollectPaymentCancelled
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.PaymentFlowTrackerEvent.CardPresentCollectPaymentFailed
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEventConstant.ReaderReadySignal
import org.assertj.core.api.Assertions.assertThat
import org.mockito.kotlin.mock
import kotlin.test.Test

class WooPosPaymentsFlowTrackerEventProviderTest {
    private val keeper = WooPosAnalyticsTrackingDataKeeper()
    private val provider = WooPosPaymentsFlowTrackerEventProvider(keeper, mock())

    @Test
    fun `given a timed payment, when outcome events are built, then success, failure and cancellation share the timing properties`() {
        // GIVEN
        val now = System.currentTimeMillis()
        keeper.startCustomerInteraction(timestamp = now - 10_000)
        keeper.orderSyncSuccessTimestamp = now - 9_000
        keeper.onReaderConnectionChanged(isConnected = true, timestamp = now - 8_000)
        keeper.readerReadyForPaymentTimestamp = now - 5_000
        keeper.readerReadySignal = ReaderReadySignal.INPUT_REQUESTED
        keeper.cardTappedTimestamp = now - 2_000

        // WHEN
        val outcomes = listOf(
            provider.CARD_PRESENT_COLLECT_PAYMENT_SUCCESS,
            provider.CARD_PRESENT_COLLECT_PAYMENT_FAILED,
            provider.CARD_PRESENT_COLLECT_PAYMENT_CANCELLED,
        ).map { it.wooPosProperties() }

        // THEN
        outcomes.forEach { properties ->
            assertThat(properties).containsKeys(
                "milliseconds_since_customer_interaction_started",
                "milliseconds_since_order_sync_success",
                "milliseconds_since_reader_ready_to_collect_payment",
                "milliseconds_since_card_tapped",
                "checkout_tap_count",
            )
            assertThat(properties["reader_connected_at_checkout"]).isEqualTo("false")
            assertThat(properties["milliseconds_waiting_for_reader_connection"]).isEqualTo("2000")
            assertThat(properties["milliseconds_from_order_sync_to_reader_ready"]).isEqualTo("3000")
            assertThat(properties["reader_ready_signal"]).isEqualTo("input_requested")
        }
    }

    @Test
    fun `given a failure was built, when the next failure is built after a new interaction, then old timings do not carry over`() {
        // GIVEN
        val now = System.currentTimeMillis()
        keeper.startCustomerInteraction(timestamp = now - 10_000)
        keeper.readerReadyForPaymentTimestamp = now - 5_000
        keeper.readerReadySignal = ReaderReadySignal.INPUT_REQUESTED
        assertThat(provider.CARD_PRESENT_COLLECT_PAYMENT_FAILED.wooPosProperties()).containsKey("reader_ready_signal")

        // WHEN
        keeper.startCustomerInteraction(timestamp = now)
        val properties = provider.CARD_PRESENT_COLLECT_PAYMENT_FAILED.wooPosProperties()

        // THEN
        assertThat(properties).doesNotContainKeys(
            "milliseconds_since_reader_ready_to_collect_payment",
            "reader_ready_signal",
        )
    }

    @Test
    fun `when failure and cancellation events are built, then each call returns a new event with its Tracks name`() {
        // WHEN
        val failed = provider.CARD_PRESENT_COLLECT_PAYMENT_FAILED
        val cancelled = provider.CARD_PRESENT_COLLECT_PAYMENT_CANCELLED

        // THEN
        assertThat(failed).isInstanceOf(CardPresentCollectPaymentFailed::class.java)
        assertThat(failed.name).isEqualTo("card_present_collect_payment_failed")
        assertThat(failed).isNotSameAs(provider.CARD_PRESENT_COLLECT_PAYMENT_FAILED)
        assertThat(cancelled).isInstanceOf(CardPresentCollectPaymentCancelled::class.java)
        assertThat(cancelled.name).isEqualTo("card_present_collect_payment_cancelled")
        assertThat(cancelled).isNotSameAs(provider.CARD_PRESENT_COLLECT_PAYMENT_CANCELLED)
    }

    private fun IAnalyticsEvent.wooPosProperties() = (this as WooPosAnalyticsEvent).properties
}
