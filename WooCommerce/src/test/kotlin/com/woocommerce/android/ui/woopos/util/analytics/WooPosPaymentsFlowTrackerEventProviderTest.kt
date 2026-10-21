package com.woocommerce.android.ui.woopos.util.analytics

import com.woocommerce.android.ui.orders.OrderTestUtils
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosPaymentsFlowTrackerEventProviderTest {
    private val provider = WooPosPaymentsFlowTrackerEventProvider(
        WooPosAnalyticsTrackingDataKeeper(mock()),
        mock(),
    )

    @Test
    fun `when payment fails, then the event keeps sync strategy without a timing version`() {
        // WHEN
        val event = provider.CARD_PRESENT_COLLECT_PAYMENT_FAILED as WooPosAnalyticsEvent

        // THEN
        assertThat(event.name).isEqualTo("card_present_collect_payment_failed")
        assertThat(event.properties).doesNotContainKey("pos_timing_version")
        assertThat(event.properties).containsEntry("sync_strategy", "unknown")
    }

    @Test
    fun `when payment is canceled, then the unchanged event has no new timing properties`() {
        // WHEN
        val event = provider.CARD_PRESENT_COLLECT_PAYMENT_CANCELLED as WooPosAnalyticsEvent

        // THEN
        assertThat(event.name).isEqualTo("card_present_collect_payment_cancelled")
        assertThat(event.properties).doesNotContainKeys(
            "pos_timing_version",
            "sync_strategy",
            "pos_time_since_checkout_started_ms",
            "pos_time_since_order_sync_completed_ms",
            "pos_time_since_payment_confirmed_ms",
        )
    }

    @Test
    fun `when payment succeeds, then legacy properties and zero timing remain without a timing version`() {
        // GIVEN
        val clock = mock<WooPosElapsedTime>()
        whenever(clock.milliseconds()).thenReturn(100L)
        val keeper = WooPosAnalyticsTrackingDataKeeper(clock).apply {
            startCheckoutTiming("local_catalog")
            val now = System.currentTimeMillis()
            interactionWithCustomerStartedTimestamp = now
            orderSyncSuccessTimestamp = now
            readerReadyForPaymentTimestamp = now
            cardTappedTimestamp = now
            checkoutButtonTapsCount = 3
        }
        val events = WooPosPaymentsFlowTrackerEventProvider(keeper, mock())

        // WHEN
        val event = events.CARD_PRESENT_COLLECT_PAYMENT_SUCCESS as WooPosAnalyticsEvent

        // THEN
        assertThat(event.name).isEqualTo("card_present_collect_payment_success")
        assertThat(event.properties).doesNotContainKey("pos_timing_version")
            .containsEntry("sync_strategy", "local_catalog")
            .containsEntry("pos_time_since_checkout_started_ms", 0L)
            .doesNotContainKey("pos_time_since_order_sync_completed_ms")
            .doesNotContainKey("pos_time_since_payment_confirmed_ms")
            .containsEntry("checkout_tap_count", "3")
        listOf(
            "milliseconds_since_customer_interaction_started",
            "milliseconds_since_order_sync_success",
            "milliseconds_since_reader_ready_to_collect_payment",
            "milliseconds_since_card_tapped",
        ).forEach { key ->
            assertThat(event.properties[key]).isInstanceOf(String::class.java)
            assertThat(event.properties[key] as String).matches("-?\\d+")
        }
    }

    @Test
    fun `when two payments fail, then their event snapshots are independent`() {
        // WHEN
        val first = provider.CARD_PRESENT_COLLECT_PAYMENT_FAILED as WooPosAnalyticsEvent
        val second = provider.CARD_PRESENT_COLLECT_PAYMENT_FAILED as WooPosAnalyticsEvent

        // THEN
        assertThat(second).isNotSameAs(first)
    }

    @Test
    fun `when all stages success, then timing properties match the shared contract`() {
        // GIVEN
        val clock = mock<WooPosElapsedTime>()
        val keeper = WooPosAnalyticsTrackingDataKeeper(clock)
        val events = WooPosPaymentsFlowTrackerEventProvider(keeper, mock())
        whenever(clock.milliseconds()).thenReturn(0L)
        val generation = keeper.startCheckoutTiming("local_catalog")
        whenever(clock.milliseconds()).thenReturn(100L)
        keeper.orderSyncCompleted(generation)
        whenever(clock.milliseconds()).thenReturn(150L)
        keeper.readerReadyForPaymentTimestamp = 150L
        whenever(clock.milliseconds()).thenReturn(200L)
        events.onConfirmedIntentAvailable()
        // WHEN
        whenever(clock.milliseconds()).thenReturn(250L)
        val actual = (events.CARD_PRESENT_COLLECT_PAYMENT_SUCCESS as WooPosAnalyticsEvent).properties
            .filterKeys { it.startsWith("pos_") || it == "sync_strategy" }
        // THEN
        assertThat(actual).doesNotContainKey("pos_time_since_reader_ready_ms")
        assertThat(actual).isEqualTo(
            mapOf(
                "sync_strategy" to "local_catalog",
                "pos_time_since_checkout_started_ms" to 250L,
                "pos_time_since_order_sync_completed_ms" to 150L,
                "pos_time_since_payment_confirmed_ms" to 50L,
            )
        )
    }

    @Test
    fun `when failure before sync, then timing properties match the shared contract`() {
        // GIVEN
        val clock = mock<WooPosElapsedTime>()
        val keeper = WooPosAnalyticsTrackingDataKeeper(clock)
        val events = WooPosPaymentsFlowTrackerEventProvider(keeper, mock())
        whenever(clock.milliseconds()).thenReturn(0L)
        keeper.startCheckoutTiming("remote")
        // WHEN
        whenever(clock.milliseconds()).thenReturn(25L)
        val actual = (events.CARD_PRESENT_COLLECT_PAYMENT_FAILED as WooPosAnalyticsEvent).properties
            .filterKeys { it.startsWith("pos_") || it == "sync_strategy" }
        // THEN
        assertThat(actual).doesNotContainKey("pos_time_since_reader_ready_ms")
        assertThat(actual).isEqualTo(
            mapOf(
                "sync_strategy" to "remote",
                "pos_time_since_checkout_started_ms" to 25L,
            )
        )
    }

    @Test
    fun `when immediate event zero is valid, then timing properties match the shared contract`() {
        // GIVEN
        val clock = mock<WooPosElapsedTime>()
        val keeper = WooPosAnalyticsTrackingDataKeeper(clock)
        val events = WooPosPaymentsFlowTrackerEventProvider(keeper, mock())
        whenever(clock.milliseconds()).thenReturn(0L)
        val generation = keeper.startCheckoutTiming("unknown")
        whenever(clock.milliseconds()).thenReturn(0L)
        keeper.orderSyncCompleted(generation)
        whenever(clock.milliseconds()).thenReturn(0L)
        keeper.readerReadyForPaymentTimestamp = 150L
        whenever(clock.milliseconds()).thenReturn(0L)
        events.onConfirmedIntentAvailable()
        // WHEN
        whenever(clock.milliseconds()).thenReturn(0L)
        val actual = (events.CARD_PRESENT_COLLECT_PAYMENT_SUCCESS as WooPosAnalyticsEvent).properties
            .filterKeys { it.startsWith("pos_") || it == "sync_strategy" }
        // THEN
        assertThat(actual).doesNotContainKey("pos_time_since_reader_ready_ms")
        assertThat(actual).isEqualTo(
            mapOf(
                "sync_strategy" to "unknown",
                "pos_time_since_checkout_started_ms" to 0L,
                "pos_time_since_order_sync_completed_ms" to 0L,
                "pos_time_since_payment_confirmed_ms" to 0L,
            )
        )
    }

    @Test
    fun `when unchanged order new checkout, then timing properties match the shared contract`() {
        // GIVEN
        val clock = mock<WooPosElapsedTime>()
        val keeper = WooPosAnalyticsTrackingDataKeeper(clock)
        val events = WooPosPaymentsFlowTrackerEventProvider(keeper, mock())
        whenever(clock.milliseconds()).thenReturn(0L)
        keeper.orderSyncCompleted(keeper.startCheckoutTiming("local_catalog"))
        whenever(clock.milliseconds()).thenReturn(500L)
        keeper.startCheckoutTiming("local_catalog")
        whenever(clock.milliseconds()).thenReturn(510L)
        keeper.readerReadyForPaymentTimestamp = 150L
        whenever(clock.milliseconds()).thenReturn(520L)
        events.onConfirmedIntentAvailable()
        // WHEN
        whenever(clock.milliseconds()).thenReturn(550L)
        val actual = (events.CARD_PRESENT_COLLECT_PAYMENT_SUCCESS as WooPosAnalyticsEvent).properties
            .filterKeys { it.startsWith("pos_") || it == "sync_strategy" }
        // THEN
        assertThat(actual).doesNotContainKey("pos_time_since_reader_ready_ms")
        assertThat(actual).isEqualTo(
            mapOf(
                "sync_strategy" to "local_catalog",
                "pos_time_since_checkout_started_ms" to 50L,
                "pos_time_since_payment_confirmed_ms" to 30L,
            )
        )
    }

    @Test
    fun `when direct retry before reader, then timing properties match the shared contract`() {
        // GIVEN
        val clock = mock<WooPosElapsedTime>()
        val keeper = WooPosAnalyticsTrackingDataKeeper(clock)
        val events = WooPosPaymentsFlowTrackerEventProvider(keeper, mock())
        whenever(clock.milliseconds()).thenReturn(0L)
        val generation = keeper.startCheckoutTiming("local_catalog")
        whenever(clock.milliseconds()).thenReturn(100L)
        keeper.orderSyncCompleted(generation)

        keeper.paymentConfirmed()
        events.CARD_PRESENT_COLLECT_PAYMENT_FAILED
        // WHEN
        whenever(clock.milliseconds()).thenReturn(350L)
        val actual = (events.CARD_PRESENT_COLLECT_PAYMENT_FAILED as WooPosAnalyticsEvent).properties
            .filterKeys { it.startsWith("pos_") || it == "sync_strategy" }
        // THEN
        assertThat(actual).doesNotContainKey("pos_time_since_reader_ready_ms")
        assertThat(actual).isEqualTo(
            mapOf(
                "sync_strategy" to "local_catalog",
                "pos_time_since_checkout_started_ms" to 350L,
                "pos_time_since_order_sync_completed_ms" to 250L,
            )
        )
    }

    @Test
    fun `when already confirmed intent reused, then timing properties match the shared contract`() {
        // GIVEN
        val clock = mock<WooPosElapsedTime>()
        val keeper = WooPosAnalyticsTrackingDataKeeper(clock)
        val events = WooPosPaymentsFlowTrackerEventProvider(keeper, mock())
        whenever(clock.milliseconds()).thenReturn(0L)
        val generation = keeper.startCheckoutTiming("local_catalog")
        whenever(clock.milliseconds()).thenReturn(100L)
        keeper.orderSyncCompleted(generation)
        whenever(clock.milliseconds()).thenReturn(400L)
        events.onConfirmedIntentAvailable()
        // WHEN
        whenever(clock.milliseconds()).thenReturn(450L)
        val actual = (events.CARD_PRESENT_COLLECT_PAYMENT_SUCCESS as WooPosAnalyticsEvent).properties
            .filterKeys { it.startsWith("pos_") || it == "sync_strategy" }
        // THEN
        assertThat(actual).doesNotContainKey("pos_time_since_reader_ready_ms")
        assertThat(actual).isEqualTo(
            mapOf(
                "sync_strategy" to "local_catalog",
                "pos_time_since_checkout_started_ms" to 450L,
                "pos_time_since_order_sync_completed_ms" to 350L,
                "pos_time_since_payment_confirmed_ms" to 50L,
            )
        )
    }

    @Test
    fun `given checkout stages, when canceled, then no new timing or strategy properties are added`() {
        // GIVEN
        val clock = mock<WooPosElapsedTime>()
        val keeper = WooPosAnalyticsTrackingDataKeeper(clock)
        val events = WooPosPaymentsFlowTrackerEventProvider(keeper, mock())
        whenever(clock.milliseconds()).thenReturn(0L)
        val generation = keeper.startCheckoutTiming("local_catalog")
        whenever(clock.milliseconds()).thenReturn(100L)
        keeper.orderSyncCompleted(generation)
        whenever(clock.milliseconds()).thenReturn(150L)
        keeper.readerReadyForPaymentTimestamp = 150L
        // WHEN
        whenever(clock.milliseconds()).thenReturn(200L)
        val actual = (events.CARD_PRESENT_COLLECT_PAYMENT_CANCELLED as WooPosAnalyticsEvent).properties
            .filterKeys { it.startsWith("pos_") || it == "sync_strategy" }
        // THEN
        assertThat(actual).isEmpty()
    }

    @Test
    fun `when failure after confirmation, then timing properties match the shared contract`() {
        // GIVEN
        val clock = mock<WooPosElapsedTime>()
        val keeper = WooPosAnalyticsTrackingDataKeeper(clock)
        val events = WooPosPaymentsFlowTrackerEventProvider(keeper, mock())
        whenever(clock.milliseconds()).thenReturn(0L)
        val generation = keeper.startCheckoutTiming("local_catalog")
        whenever(clock.milliseconds()).thenReturn(100L)
        keeper.orderSyncCompleted(generation)
        whenever(clock.milliseconds()).thenReturn(150L)
        keeper.readerReadyForPaymentTimestamp = 150L
        whenever(clock.milliseconds()).thenReturn(200L)
        events.onConfirmedIntentAvailable()
        // WHEN
        whenever(clock.milliseconds()).thenReturn(250L)
        val actual = (events.CARD_PRESENT_COLLECT_PAYMENT_FAILED as WooPosAnalyticsEvent).properties
            .filterKeys { it.startsWith("pos_") || it == "sync_strategy" }
        // THEN
        assertThat(actual).doesNotContainKey("pos_time_since_reader_ready_ms")
        assertThat(actual).isEqualTo(
            mapOf(
                "sync_strategy" to "local_catalog",
                "pos_time_since_checkout_started_ms" to 250L,
                "pos_time_since_order_sync_completed_ms" to 150L,
                "pos_time_since_payment_confirmed_ms" to 50L,
            )
        )
    }

    @Test
    fun `when cash success card stages always absent, then timing properties match the shared contract`() {
        // GIVEN
        val clock = mock<WooPosElapsedTime>()
        val keeper = WooPosAnalyticsTrackingDataKeeper(clock)
        val events = WooPosPaymentsFlowTrackerEventProvider(keeper, mock())
        whenever(clock.milliseconds()).thenReturn(0L)
        val generation = keeper.startCheckoutTiming("remote")
        whenever(clock.milliseconds()).thenReturn(100L)
        keeper.orderSyncCompleted(generation)
        keeper.readerReadyForPaymentTimestamp = 150L
        events.onConfirmedIntentAvailable()
        // WHEN
        whenever(clock.milliseconds()).thenReturn(300L)
        val actual = keeper.cashPaymentTiming()
        // THEN
        assertThat(actual).doesNotContainKey("pos_time_since_reader_ready_ms")
        assertThat(actual).isEqualTo(
            mapOf(
                "sync_strategy" to "remote",
                "pos_time_since_checkout_started_ms" to 300L,
                "pos_time_since_order_sync_completed_ms" to 200L,
            )
        )
    }

    @Test
    fun `when old sync completes after a new checkout, then the new checkout omits sync timing`() {
        // GIVEN
        val clock = mock<WooPosElapsedTime>()
        val keeper = WooPosAnalyticsTrackingDataKeeper(clock)
        whenever(clock.milliseconds()).thenReturn(10L)
        val old = keeper.startCheckoutTiming("remote")
        whenever(clock.milliseconds()).thenReturn(20L)
        keeper.startCheckoutTiming("local_catalog")
        // WHEN
        keeper.orderSyncCompleted(old)
        whenever(clock.milliseconds()).thenReturn(30L)
        val actual = keeper.cardPaymentTiming()
        // THEN
        assertThat(actual).containsEntry("pos_time_since_checkout_started_ms", 10L)
            .doesNotContainKey("pos_time_since_order_sync_completed_ms")
    }

    @Test
    fun `when confirmation repeats, then its first timestamp is retained and the outcome clears it`() {
        // GIVEN
        val clock = mock<WooPosElapsedTime>()
        val keeper = WooPosAnalyticsTrackingDataKeeper(clock)
        whenever(clock.milliseconds()).thenReturn(0L)
        keeper.startCheckoutTiming("remote")
        whenever(clock.milliseconds()).thenReturn(10L)

        keeper.paymentConfirmed()
        whenever(clock.milliseconds()).thenReturn(20L)

        keeper.paymentConfirmed()
        // WHEN
        val actual = keeper.cardPaymentTiming()
        val duplicate = keeper.cardPaymentTiming()
        // THEN
        assertThat(actual).containsEntry("pos_time_since_payment_confirmed_ms", 10L)
        assertThat(duplicate).containsEntry("pos_time_since_checkout_started_ms", 20L)
            .doesNotContainKey("pos_time_since_payment_confirmed_ms")
    }

    @Test
    fun `when elapsed time is negative, then invalid durations are omitted`() {
        // GIVEN
        val clock = mock<WooPosElapsedTime>()
        val keeper = WooPosAnalyticsTrackingDataKeeper(clock)
        whenever(clock.milliseconds()).thenReturn(10L)
        keeper.startCheckoutTiming("unknown")

        // WHEN
        whenever(clock.milliseconds()).thenReturn(5L)
        val actual = keeper.cardPaymentTiming()
        // THEN
        assertThat(actual).isEqualTo(mapOf("sync_strategy" to "unknown"))
    }

    @Test
    fun `when customer context is reset, then late terminal events omit old checkout timing`() {
        // GIVEN
        val clock = mock<WooPosElapsedTime>()
        val keeper = WooPosAnalyticsTrackingDataKeeper(clock)
        val oldGeneration = keeper.startCheckoutTiming("remote")
        keeper.orderSyncCompleted(oldGeneration)

        // WHEN
        keeper.reset()
        keeper.orderSyncCompleted(oldGeneration)
        val actual = keeper.cardPaymentTiming()
        // THEN
        assertThat(actual).isEqualTo(mapOf("sync_strategy" to "unknown"))
    }

    @Test
    fun `given delayed metadata, when success is tracked, then snapshot includes metadata lookup time`() = runTest {
        // GIVEN
        val clock = mock<WooPosElapsedTime>()
        val keeper = WooPosAnalyticsTrackingDataKeeper(clock)
        val properties = mock<WooPosPaymentSuccessProperties>()
        val order = OrderTestUtils.generateTestOrder()
        keeper.startCheckoutTiming("remote")
        val metadata = CompletableDeferred<Map<String, Any>>()
        whenever(properties(order)).doSuspendableAnswer { metadata.await() }
        val events = WooPosPaymentsFlowTrackerEventProvider(keeper, properties)
        // WHEN
        val terminal = async { events.paymentSuccessEvent(order) }
        runCurrent()
        assertThat(terminal.isCompleted).isFalse()
        whenever(clock.milliseconds()).thenReturn(350L)
        metadata.complete(emptyMap())
        val actual = terminal.await() as WooPosAnalyticsEvent
        // THEN
        assertThat(actual.properties).containsEntry("pos_time_since_checkout_started_ms", 350L)
    }

    @Test
    fun `given confirmed card collection, when cash succeeds, then card timing remains available`() {
        // GIVEN
        val clock = mock<WooPosElapsedTime>()
        val keeper = WooPosAnalyticsTrackingDataKeeper(clock)
        keeper.startCheckoutTiming("remote")
        whenever(clock.milliseconds()).thenReturn(100L)
        keeper.paymentConfirmed()

        // WHEN
        whenever(clock.milliseconds()).thenReturn(200L)
        val cash = keeper.cashPaymentTiming()
        whenever(clock.milliseconds()).thenReturn(300L)
        val card = keeper.cardPaymentTiming()

        // THEN
        assertThat(cash).containsEntry("pos_time_since_checkout_started_ms", 200L)
            .doesNotContainKey("pos_time_since_payment_confirmed_ms")
        assertThat(card).containsEntry("pos_time_since_checkout_started_ms", 300L)
            .containsEntry("pos_time_since_payment_confirmed_ms", 200L)
    }

    @Test
    fun `given confirmed collection, when canceled and retried, then confirmation is not reused`() {
        // GIVEN
        val clock = mock<WooPosElapsedTime>()
        val keeper = WooPosAnalyticsTrackingDataKeeper(clock)
        val events = WooPosPaymentsFlowTrackerEventProvider(keeper, mock())
        keeper.startCheckoutTiming("local_catalog")
        whenever(clock.milliseconds()).thenReturn(100L)
        keeper.paymentConfirmed()

        // WHEN
        val canceled = events.CARD_PRESENT_COLLECT_PAYMENT_CANCELLED as WooPosAnalyticsEvent
        whenever(clock.milliseconds()).thenReturn(200L)
        val retry = events.CARD_PRESENT_COLLECT_PAYMENT_FAILED as WooPosAnalyticsEvent

        // THEN
        assertThat(canceled.properties).doesNotContainKeys(
            "sync_strategy",
            "pos_time_since_checkout_started_ms",
            "pos_time_since_order_sync_completed_ms",
            "pos_time_since_payment_confirmed_ms",
        )
        assertThat(retry.properties).containsEntry("pos_time_since_checkout_started_ms", 200L)
            .doesNotContainKey("pos_time_since_payment_confirmed_ms")
    }

    @Test
    fun `given previous checkout stages, when checkout restarts, then only the new checkout is timed`() {
        // GIVEN
        val clock = mock<WooPosElapsedTime>()
        val keeper = WooPosAnalyticsTrackingDataKeeper(clock)
        keeper.orderSyncCompleted(keeper.startCheckoutTiming("remote"))
        keeper.paymentConfirmed()

        // WHEN
        whenever(clock.milliseconds()).thenReturn(100L)
        keeper.startCheckoutTiming("local_catalog")
        whenever(clock.milliseconds()).thenReturn(150L)
        val actual = keeper.cardPaymentTiming()

        // THEN
        assertThat(actual).isEqualTo(
            mapOf("sync_strategy" to "local_catalog", "pos_time_since_checkout_started_ms" to 50L)
        )
    }

    @Test
    fun `given no checkout, when confirmation arrives, then no elapsed timing is created`() {
        // GIVEN
        val clock = mock<WooPosElapsedTime>()
        val keeper = WooPosAnalyticsTrackingDataKeeper(clock)

        // WHEN
        keeper.paymentConfirmed()
        whenever(clock.milliseconds()).thenReturn(100L)
        val actual = keeper.cardPaymentTiming()

        // THEN
        assertThat(actual).isEqualTo(mapOf("sync_strategy" to "unknown"))
    }

    @Test
    fun `given legacy state, when reset, then customer timers and checkout counter are cleared`() {
        // GIVEN
        val keeper = WooPosAnalyticsTrackingDataKeeper(mock()).apply {
            interactionWithCustomerStartedTimestamp = 1L
            orderSyncSuccessTimestamp = 2L
            readerReadyForPaymentTimestamp = 3L
            cardTappedTimestamp = 4L
            checkoutButtonTapsCount = 5
        }

        // WHEN
        keeper.reset()

        // THEN
        assertThat(keeper.interactionWithCustomerStartedTimestamp).isNull()
        assertThat(keeper.orderSyncSuccessTimestamp).isNull()
        assertThat(keeper.readerReadyForPaymentTimestamp).isNull()
        assertThat(keeper.cardTappedTimestamp).isNull()
        assertThat(keeper.checkoutButtonTapsCount).isZero()
    }
}
