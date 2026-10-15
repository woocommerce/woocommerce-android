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
        WooPosAnalyticsTrackingDataKeeper(),
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
    fun `when payment is canceled, then the event keeps sync strategy without a timing version`() {
        // WHEN
        val event = provider.CARD_PRESENT_COLLECT_PAYMENT_CANCELLED as WooPosAnalyticsEvent

        // THEN
        assertThat(event.name).isEqualTo("card_present_collect_payment_cancelled")
        assertThat(event.properties).doesNotContainKey("pos_timing_version")
        assertThat(event.properties).containsEntry("sync_strategy", "unknown")
    }

    @Test
    fun `when payment succeeds, then legacy properties and zero timing remain without a timing version`() {
        // GIVEN
        val clock = mock<WooPosElapsedTime>()
        whenever(clock.milliseconds()).thenReturn(100L)
        val timing = WooPosCheckoutTiming(clock)
        timing.startCheckout("local_catalog")
        val keeper = WooPosAnalyticsTrackingDataKeeper(timing).apply {
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
            assertThat((event.properties[key] as String).toLong()).isGreaterThanOrEqualTo(0L)
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
        val timing = WooPosCheckoutTiming(clock)
        val keeper = WooPosAnalyticsTrackingDataKeeper(timing)
        val events = WooPosPaymentsFlowTrackerEventProvider(keeper, mock())
        whenever(clock.milliseconds()).thenReturn(0L)
        val generation = timing.startCheckout("local_catalog")
        whenever(clock.milliseconds()).thenReturn(100L)
        timing.orderSyncCompleted(generation)
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
        val timing = WooPosCheckoutTiming(clock)
        val keeper = WooPosAnalyticsTrackingDataKeeper(timing)
        val events = WooPosPaymentsFlowTrackerEventProvider(keeper, mock())
        whenever(clock.milliseconds()).thenReturn(0L)
        val generation = timing.startCheckout("remote")
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
        val timing = WooPosCheckoutTiming(clock)
        val keeper = WooPosAnalyticsTrackingDataKeeper(timing)
        val events = WooPosPaymentsFlowTrackerEventProvider(keeper, mock())
        whenever(clock.milliseconds()).thenReturn(0L)
        val generation = timing.startCheckout("unknown")
        whenever(clock.milliseconds()).thenReturn(0L)
        timing.orderSyncCompleted(generation)
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
        val timing = WooPosCheckoutTiming(clock)
        val keeper = WooPosAnalyticsTrackingDataKeeper(timing)
        val events = WooPosPaymentsFlowTrackerEventProvider(keeper, mock())
        whenever(clock.milliseconds()).thenReturn(0L)
        timing.orderSyncCompleted(timing.startCheckout("local_catalog"))
        whenever(clock.milliseconds()).thenReturn(500L)
        val generation = timing.startCheckout("local_catalog")
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
        val timing = WooPosCheckoutTiming(clock)
        val keeper = WooPosAnalyticsTrackingDataKeeper(timing)
        val events = WooPosPaymentsFlowTrackerEventProvider(keeper, mock())
        whenever(clock.milliseconds()).thenReturn(0L)
        val generation = timing.startCheckout("local_catalog")
        whenever(clock.milliseconds()).thenReturn(100L)
        timing.orderSyncCompleted(generation)

        timing.confirmedIntentAvailable()
        events.CARD_PRESENT_COLLECT_PAYMENT_FAILED
        events.forPaymentAttempt()
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
        val timing = WooPosCheckoutTiming(clock)
        val keeper = WooPosAnalyticsTrackingDataKeeper(timing)
        val events = WooPosPaymentsFlowTrackerEventProvider(keeper, mock())
        whenever(clock.milliseconds()).thenReturn(0L)
        val generation = timing.startCheckout("local_catalog")
        whenever(clock.milliseconds()).thenReturn(100L)
        timing.orderSyncCompleted(generation)
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
    fun `when cancel after reader ready, then timing properties match the shared contract`() {
        // GIVEN
        val clock = mock<WooPosElapsedTime>()
        val timing = WooPosCheckoutTiming(clock)
        val keeper = WooPosAnalyticsTrackingDataKeeper(timing)
        val events = WooPosPaymentsFlowTrackerEventProvider(keeper, mock())
        whenever(clock.milliseconds()).thenReturn(0L)
        val generation = timing.startCheckout("local_catalog")
        whenever(clock.milliseconds()).thenReturn(100L)
        timing.orderSyncCompleted(generation)
        whenever(clock.milliseconds()).thenReturn(150L)
        keeper.readerReadyForPaymentTimestamp = 150L
        // WHEN
        whenever(clock.milliseconds()).thenReturn(200L)
        val actual = (events.CARD_PRESENT_COLLECT_PAYMENT_CANCELLED as WooPosAnalyticsEvent).properties
            .filterKeys { it.startsWith("pos_") || it == "sync_strategy" }
        // THEN
        assertThat(actual).doesNotContainKey("pos_time_since_reader_ready_ms")
        assertThat(actual).isEqualTo(
            mapOf(
                "sync_strategy" to "local_catalog",
                "pos_time_since_checkout_started_ms" to 200L,
                "pos_time_since_order_sync_completed_ms" to 100L,
            )
        )
    }

    @Test
    fun `when failure after confirmation, then timing properties match the shared contract`() {
        // GIVEN
        val clock = mock<WooPosElapsedTime>()
        val timing = WooPosCheckoutTiming(clock)
        val keeper = WooPosAnalyticsTrackingDataKeeper(timing)
        val events = WooPosPaymentsFlowTrackerEventProvider(keeper, mock())
        whenever(clock.milliseconds()).thenReturn(0L)
        val generation = timing.startCheckout("local_catalog")
        whenever(clock.milliseconds()).thenReturn(100L)
        timing.orderSyncCompleted(generation)
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
        val timing = WooPosCheckoutTiming(clock)
        val keeper = WooPosAnalyticsTrackingDataKeeper(timing)
        val events = WooPosPaymentsFlowTrackerEventProvider(keeper, mock())
        whenever(clock.milliseconds()).thenReturn(0L)
        val generation = timing.startCheckout("remote")
        whenever(clock.milliseconds()).thenReturn(100L)
        timing.orderSyncCompleted(generation)
        keeper.readerReadyForPaymentTimestamp = 150L
        events.onConfirmedIntentAvailable()
        // WHEN
        whenever(clock.milliseconds()).thenReturn(300L)
        val actual = timing.terminalSnapshot(success = true, includeCardStages = false)
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
        val timing = WooPosCheckoutTiming(clock)
        whenever(clock.milliseconds()).thenReturn(10L)
        val old = timing.startCheckout("remote")
        whenever(clock.milliseconds()).thenReturn(20L)
        timing.startCheckout("local_catalog")
        // WHEN
        timing.orderSyncCompleted(old)
        whenever(clock.milliseconds()).thenReturn(30L)
        val actual = timing.terminalSnapshot(success = true)
        // THEN
        assertThat(actual).containsEntry("pos_time_since_checkout_started_ms", 10L)
            .doesNotContainKey("pos_time_since_order_sync_completed_ms")
    }

    @Test
    fun `when stage repeats, then its first timestamp is retained and success closes the context`() {
        // GIVEN
        val clock = mock<WooPosElapsedTime>()
        val timing = WooPosCheckoutTiming(clock)
        whenever(clock.milliseconds()).thenReturn(0L)
        timing.startCheckout("remote")
        whenever(clock.milliseconds()).thenReturn(10L)

        timing.confirmedIntentAvailable()
        whenever(clock.milliseconds()).thenReturn(20L)

        timing.confirmedIntentAvailable()
        // WHEN
        val actual = timing.terminalSnapshot(success = true)
        val duplicate = timing.terminalSnapshot(success = true)
        // THEN
        assertThat(actual).containsEntry("pos_time_since_payment_confirmed_ms", 10L)
        assertThat(duplicate).isEqualTo(mapOf("sync_strategy" to "remote"))
    }

    @Test
    fun `when elapsed time is negative, then invalid durations are omitted`() {
        // GIVEN
        val clock = mock<WooPosElapsedTime>()
        val timing = WooPosCheckoutTiming(clock)
        whenever(clock.milliseconds()).thenReturn(10L)
        timing.startCheckout("unknown")

        // WHEN
        whenever(clock.milliseconds()).thenReturn(5L)
        val actual = timing.terminalSnapshot(success = false)
        // THEN
        assertThat(actual).isEqualTo(mapOf("sync_strategy" to "unknown"))
    }

    @Test
    fun `when customer context is reset, then late terminal events omit old checkout timing`() {
        // GIVEN
        val clock = mock<WooPosElapsedTime>()
        val timing = WooPosCheckoutTiming(clock)
        val keeper = WooPosAnalyticsTrackingDataKeeper(timing)
        val oldGeneration = timing.startCheckout("remote")
        timing.orderSyncCompleted(oldGeneration)

        // WHEN
        keeper.reset()
        timing.orderSyncCompleted(oldGeneration)
        val actual = timing.terminalSnapshot(success = false)
        // THEN
        assertThat(actual).isEqualTo(mapOf("sync_strategy" to "remote"))
    }

    @Test
    fun `given delayed metadata, when success is tracked, then snapshot includes metadata lookup time`() = runTest {
        // GIVEN
        val clock = mock<WooPosElapsedTime>()
        val timing = WooPosCheckoutTiming(clock)
        val properties = mock<WooPosPaymentSuccessProperties>()
        val order = OrderTestUtils.generateTestOrder()
        timing.startCheckout("remote")
        val metadata = CompletableDeferred<Map<String, Any>>()
        whenever(properties(order)).doSuspendableAnswer { metadata.await() }
        val events = WooPosPaymentsFlowTrackerEventProvider(WooPosAnalyticsTrackingDataKeeper(timing), properties)
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
    fun `given held old success, when a new checkout starts, then old timing keeps its own context`() = runTest {
        // GIVEN
        val clock = mock<WooPosElapsedTime>()
        val timing = WooPosCheckoutTiming(clock)
        timing.startCheckout("remote")
        val properties = mock<WooPosPaymentSuccessProperties>()
        val order = OrderTestUtils.generateTestOrder()
        val metadata = CompletableDeferred<Map<String, Any>>()
        whenever(properties(order)).doSuspendableAnswer { metadata.await() }
        val events = WooPosPaymentsFlowTrackerEventProvider(WooPosAnalyticsTrackingDataKeeper(timing), properties)
        val oldSuccess = async { events.paymentSuccessEvent(order) }
        runCurrent()
        // WHEN
        whenever(clock.milliseconds()).thenReturn(100L)
        timing.startCheckout("local_catalog")
        whenever(clock.milliseconds()).thenReturn(200L)
        metadata.complete(emptyMap())
        val old = oldSuccess.await() as WooPosAnalyticsEvent
        val current = timing.terminalSnapshot(success = false)
        // THEN
        assertThat(old.properties).containsEntry("sync_strategy", "remote")
            .containsEntry("pos_time_since_checkout_started_ms", 200L)
        assertThat(current).containsEntry("sync_strategy", "local_catalog")
            .containsEntry("pos_time_since_checkout_started_ms", 100L)
    }

    @Test
    fun `given old cancel and confirmed callbacks, when another collection starts, then new timings stay unchanged`() {
        // GIVEN
        val clock = mock<WooPosElapsedTime>()
        val timing = WooPosCheckoutTiming(clock)
        timing.startCheckout("remote")
        val base = WooPosPaymentsFlowTrackerEventProvider(WooPosAnalyticsTrackingDataKeeper(timing), mock())
        val old = base.forPaymentAttempt() as WooPosPaymentsFlowTrackerEventProvider
        whenever(clock.milliseconds()).thenReturn(10L)
        old.onConfirmedIntentAvailable()
        val delayedCancel = { old.CARD_PRESENT_COLLECT_PAYMENT_CANCELLED as WooPosAnalyticsEvent }
        whenever(clock.milliseconds()).thenReturn(100L)
        val next = base.forPaymentAttempt() as WooPosPaymentsFlowTrackerEventProvider
        whenever(clock.milliseconds()).thenReturn(120L)
        next.onConfirmedIntentAvailable()
        // WHEN
        whenever(clock.milliseconds()).thenReturn(150L)
        val cancelled = delayedCancel()
        whenever(clock.milliseconds()).thenReturn(160L)
        old.onConfirmedIntentAvailable()
        whenever(clock.milliseconds()).thenReturn(200L)
        val succeeded = next.CARD_PRESENT_COLLECT_PAYMENT_SUCCESS as WooPosAnalyticsEvent
        // THEN
        assertThat(cancelled.properties).containsEntry("pos_time_since_payment_confirmed_ms", 140L)
        assertThat(succeeded.properties).containsEntry("pos_time_since_checkout_started_ms", 200L)
            .containsEntry("pos_time_since_payment_confirmed_ms", 80L)
    }

    @Test
    fun `given held old success, when a new collection shares checkout, then success closes only its own collection`() = runTest {
        // GIVEN
        val clock = mock<WooPosElapsedTime>()
        val timing = WooPosCheckoutTiming(clock)
        timing.startCheckout("remote")
        val metadata = CompletableDeferred<Map<String, Any>>()
        val properties = mock<WooPosPaymentSuccessProperties>()
        val order = OrderTestUtils.generateTestOrder()
        whenever(properties(order)).doSuspendableAnswer { metadata.await() }
        val base = WooPosPaymentsFlowTrackerEventProvider(WooPosAnalyticsTrackingDataKeeper(timing), properties)
        val old = base.forPaymentAttempt() as WooPosPaymentsFlowTrackerEventProvider
        val delayedSuccess = async { old.paymentSuccessEvent(order) }
        runCurrent()
        // WHEN
        whenever(clock.milliseconds()).thenReturn(100L)
        val next = base.forPaymentAttempt() as WooPosPaymentsFlowTrackerEventProvider
        whenever(clock.milliseconds()).thenReturn(120L)
        next.onConfirmedIntentAvailable()
        whenever(clock.milliseconds()).thenReturn(150L)
        metadata.complete(emptyMap())
        delayedSuccess.await()
        whenever(clock.milliseconds()).thenReturn(200L)
        val nextSuccess = next.CARD_PRESENT_COLLECT_PAYMENT_SUCCESS as WooPosAnalyticsEvent
        val duplicate = old.CARD_PRESENT_COLLECT_PAYMENT_SUCCESS as WooPosAnalyticsEvent
        // THEN
        assertThat(nextSuccess.properties).containsEntry("pos_time_since_checkout_started_ms", 200L)
            .containsEntry("pos_time_since_payment_confirmed_ms", 80L)
        assertThat(duplicate.properties).doesNotContainKey("pos_time_since_checkout_started_ms")
    }
}
